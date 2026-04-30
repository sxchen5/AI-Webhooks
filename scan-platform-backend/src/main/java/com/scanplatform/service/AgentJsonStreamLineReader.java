package com.scanplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * 从 agent {@code --output-format stream-json} 的 stdout 中读取 JSON 对象。
 * <p>按行解析（NDJSON）：每行一个完整 JSON 对象。非 JSON 行（如 agent 输出的 {@code Error: ...} 提示）跳过并打日志，避免整流解析因夹杂纯文本而失败。
 */
@Slf4j
public final class AgentJsonStreamLineReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AgentJsonStreamLineReader() {
    }

    /**
     * 从进程输出流按行解析 JSON 根对象并回调。
     */
    public static void parseStream(InputStream in, Consumer<JsonNode> onRoot) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty()) {
                    continue;
                }
                if (!t.startsWith("{")) {
                    log.warn("Git 问答 agent 输出行非 JSON（已跳过）: {}", preview(t, 400));
                    continue;
                }
                try {
                    JsonNode root = MAPPER.readTree(t);
                    onRoot.accept(root);
                } catch (Exception e) {
                    log.warn("Git 问答 agent JSON 行解析失败（已跳过）: {} err={}", preview(t, 240), e.getMessage());
                }
            }
        }
    }

    private static String preview(String s, int max) {
        if (s == null) {
            return "";
        }
        String oneLine = s.replace('\n', ' ').replace('\r', ' ');
        if (oneLine.length() <= max) {
            return oneLine;
        }
        return oneLine.substring(0, max) + "…";
    }
}
