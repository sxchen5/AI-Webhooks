package com.scanplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

/**
 * 解析 agent {@code --output-format stream-json} 每行 JSON，抽取可展示的助手/思考文本增量。
 */
public final class AgentStreamJsonSseExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final StringBuilder assistantAccum = new StringBuilder();
    private final StringBuilder thinkingAccum = new StringBuilder();

    /**
     * 解析已由 Jackson 解析好的 stream-json 根对象（与 {@link #consumeLine} 逻辑一致）。
     */
    public String consumeRoot(JsonNode root) {
        if (root == null || !root.isObject()) {
            return "";
        }
        try {
            String type = root.path("type").asText("");
            if ("thinking".equals(type) && "delta".equals(root.path("subtype").asText(""))) {
                String delta = root.path("text").asText("");
                if (!StringUtils.hasText(delta)) {
                    return "";
                }
                thinkingAccum.append(delta);
                return delta;
            }
            if ("assistant".equals(type)) {
                JsonNode content = root.path("message").path("content");
                if (!content.isArray()) {
                    return "";
                }
                StringBuilder piece = new StringBuilder();
                for (JsonNode block : content) {
                    if ("text".equals(block.path("type").asText(""))) {
                        piece.append(block.path("text").asText(""));
                    }
                }
                String full = piece.toString();
                if (!StringUtils.hasText(full)) {
                    return "";
                }
                String prev = assistantAccum.toString();
                if (full.startsWith(prev)) {
                    String delta = full.substring(prev.length());
                    assistantAccum.setLength(0);
                    assistantAccum.append(full);
                    return delta;
                }
                assistantAccum.setLength(0);
                assistantAccum.append(full);
                return full;
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /**
     * @return 本行对应的新增展示文本（可能为空）
     */
    public String consumeLine(String rawLine) {
        if (!StringUtils.hasText(rawLine)) {
            return "";
        }
        String line = rawLine.trim();
        if (!line.startsWith("{")) {
            return "";
        }
        try {
            JsonNode root = MAPPER.readTree(line);
            return consumeRoot(root);
        } catch (Exception ignored) {
            return "";
        }
    }

    public void reset() {
        assistantAccum.setLength(0);
        thinkingAccum.setLength(0);
    }
}
