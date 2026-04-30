package com.scanplatform.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * 从 agent {@code --output-format stream-json} 的 stdout 中增量读取 JSON 对象。
 * 支持 NDJSON（每行一个对象）或同一流中多个对象紧挨（无换行），避免 {@link java.io.BufferedReader#readLine()} 整行缓冲导致前端长时间收不到 SSE。
 */
public final class AgentJsonStreamLineReader {

    private static final JsonFactory FACTORY = new JsonFactory();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AgentJsonStreamLineReader() {
    }

    /**
     * 从进程输出流解析出一个个 JSON 根对象并回调（每解析完一个即调用一次，可尽快 flush 到 SSE）。
     */
    public static void parseStream(InputStream in, Consumer<JsonNode> onRoot) throws IOException {
        try (JsonParser parser = FACTORY.createParser(in)) {
            JsonToken t;
            while ((t = parser.nextToken()) != null) {
                if (t == JsonToken.START_OBJECT) {
                    JsonNode root = MAPPER.readTree(parser);
                    onRoot.accept(root);
                }
            }
        }
    }
}
