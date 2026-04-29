package com.scanplatform.service;

/**
 * 将 agent_command 模板中的占位符替换为实际值。
 */
public final class AgentCommandUtil {

    private AgentCommandUtil() {
    }

    public static String buildCommand(String template, String path, String branch, String commit) {
        String p = path != null ? path : "";
        String b = branch != null ? branch : "";
        String c = commit != null ? commit : "";
        return template
                .replace("{{path}}", p)
                .replace("{{branch}}", b)
                .replace("{{commit}}", c);
    }
}
