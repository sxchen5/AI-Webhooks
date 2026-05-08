package com.scanplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

/**
 * 解析 agent {@code --output-format stream-json} 每行 JSON，区分助手正文与 {@code type=thinking} 思考增量。
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
                assistantAccum.setLength(0);
                assistantAccum.append(full);
                return StreamJsonDelta.assistant(full);
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
