package com.scanplatform.service;

import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Git 问答对话可选模型白名单；非空且合法时在 agent 命令末尾追加 {@code --model <id>}。
 */
public final class GitQaModelSupport {

    public static final Set<String> ALLOWED_MODELS = Set.of(
            "auto",
            "composer-2-fast",
            "composer-2",
            "composer-1.5",
            "grok-4-20",
            "grok-4-20-thinking",
            "kimi-k2.5");

    private GitQaModelSupport() {
    }

    public static String normalizeOrNull(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String m = raw.trim();
        if (!ALLOWED_MODELS.contains(m)) {
            throw new IllegalArgumentException("不支持的模型: " + m);
        }
        return m;
    }

    /**
     * 若 model 非空且命令中尚未包含 {@code --model}，则在末尾追加 {@code --model <model>}。
     */
    public static String appendModelFlag(String command, String model) {
        if (command == null) {
            return null;
        }
        String m = normalizeOrNull(model);
        if (m == null) {
            return command;
        }
        if (command.contains("--model")) {
            return command;
        }
        return command + " --model " + m;
    }
}
