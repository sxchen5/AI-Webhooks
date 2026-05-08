package com.scanplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

/**
 * 解析 agent {@code --output-format stream-json} 每行 JSON，区分助手正文与 {@code type=thinking} 思考增量；
 * 支持多段 {@code assistant}（非前缀扩展时按追加处理）及 {@code type=result} 最终汇总文本。
 */
public final class AgentStreamJsonSseExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final StringBuilder assistantAccum = new StringBuilder();
    private final StringBuilder thinkingAccum = new StringBuilder();

    /**
     * 单条 stream-json 根对象解析结果。
     *
     * @param kind   增量类型
     * @param text   新增文本（可能为空）
     */
    public record StreamJsonDelta(Kind kind, String text) {
        public enum Kind {
            NONE,
            ASSISTANT,
            THINKING
        }

        public static StreamJsonDelta none() {
            return new StreamJsonDelta(Kind.NONE, "");
        }

        public static StreamJsonDelta assistant(String t) {
            return new StreamJsonDelta(Kind.ASSISTANT, t != null ? t : "");
        }

        public static StreamJsonDelta thinking(String t) {
            return new StreamJsonDelta(Kind.THINKING, t != null ? t : "");
        }

        public boolean hasText() {
            return text != null && !text.isEmpty();
        }
    }

    /**
     * 解析已由 Jackson 解析好的 stream-json 根对象（与 {@link #consumeLine} 逻辑一致）。
     */
    public StreamJsonDelta consumeRoot(JsonNode root) {
        if (root == null || !root.isObject()) {
            return StreamJsonDelta.none();
        }
        try {
            String type = root.path("type").asText("");
            if ("thinking".equals(type) && "delta".equals(root.path("subtype").asText(""))) {
                String delta = root.path("text").asText("");
                if (!StringUtils.hasText(delta)) {
                    return StreamJsonDelta.none();
                }
                thinkingAccum.append(delta);
                return StreamJsonDelta.thinking(delta);
            }
            if ("assistant".equals(type)) {
                JsonNode content = root.path("message").path("content");
                if (!content.isArray()) {
                    return StreamJsonDelta.none();
                }
                StringBuilder piece = new StringBuilder();
                for (JsonNode block : content) {
                    if ("text".equals(block.path("type").asText(""))) {
                        piece.append(block.path("text").asText(""));
                    }
                }
                String full = piece.toString();
                if (!StringUtils.hasText(full)) {
                    return StreamJsonDelta.none();
                }
                String prev = assistantAccum.toString();
                if (full.startsWith(prev)) {
                    String delta = full.substring(prev.length());
                    assistantAccum.setLength(0);
                    assistantAccum.append(full);
                    return StringUtils.hasText(delta) ? StreamJsonDelta.assistant(delta) : StreamJsonDelta.none();
                }
                // 多段 assistant：后一段不是前一段的前缀扩展时，按追加片段处理（避免丢掉「正在读取…」等前缀）
                assistantAccum.append(full);
                return StreamJsonDelta.assistant(full);
            }
            if ("result".equals(type) && "success".equals(root.path("subtype").asText(""))) {
                String r = root.path("result").asText("");
                if (!StringUtils.hasText(r)) {
                    return StreamJsonDelta.none();
                }
                String prev = assistantAccum.toString();
                if (r.equals(prev)) {
                    return StreamJsonDelta.none();
                }
                if (r.startsWith(prev)) {
                    String delta = r.substring(prev.length());
                    assistantAccum.setLength(0);
                    assistantAccum.append(r);
                    return StringUtils.hasText(delta) ? StreamJsonDelta.assistant(delta) : StreamJsonDelta.none();
                }
                log.warn(
                        "Git 问答 stream-json result 与已累积助手正文前缀不一致，已忽略 result（避免重复下发） prevLen={} resultLen={}",
                        prev.length(),
                        r.length());
                return StreamJsonDelta.none();
            }
        } catch (Exception ignored) {
        }
        return StreamJsonDelta.none();
    }

    /**
     * @return 本行对应的增量（类型见 {@link StreamJsonDelta#kind()}）
     */
    public StreamJsonDelta consumeLine(String rawLine) {
        if (!StringUtils.hasText(rawLine)) {
            return StreamJsonDelta.none();
        }
        String line = rawLine.trim();
        if (!line.startsWith("{")) {
            return StreamJsonDelta.none();
        }
        try {
            JsonNode root = MAPPER.readTree(line);
            return consumeRoot(root);
        } catch (Exception ignored) {
            return StreamJsonDelta.none();
        }
    }

    public void reset() {
        assistantAccum.setLength(0);
        thinkingAccum.setLength(0);
    }
}
