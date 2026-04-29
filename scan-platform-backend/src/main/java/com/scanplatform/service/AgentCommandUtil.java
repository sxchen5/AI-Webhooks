package com.scanplatform.service;

/**
 * 将 agent_command 模板中的占位符替换为实际值。
 */
public final class AgentCommandUtil {

    private AgentCommandUtil() {
    }

    public static String buildCommand(String template, String path, String branch, String commit) {
        return buildCommand(template, path, branch, commit, "");
    }

    public static String buildCommand(String template, String path, String branch, String commit, String question) {
        String p = path != null ? path : "";
        String b = branch != null ? branch : "";
        String c = commit != null ? commit : "";
        String q = question != null ? question : "";
        return template
                .replace("{{path}}", p)
                .replace("{{branch}}", b)
                .replace("{{commit}}", c)
                .replace("{{question}}", q);
    }
}
