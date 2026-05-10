package com.scanplatform.agent;

import org.springframework.util.StringUtils;

/**
 * Agent 命令行工具类型：Cursor {@code agent} 与 Claude Code {@code claude}。
 */
public enum AgentCliKind {
    CURSOR,
    CLAUDE;

    public static AgentCliKind fromDb(String raw) {
        if (!StringUtils.hasText(raw)) {
            return CURSOR;
        }
        try {
            return AgentCliKind.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CURSOR;
        }
    }
}
