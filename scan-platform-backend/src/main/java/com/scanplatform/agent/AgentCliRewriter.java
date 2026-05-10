package com.scanplatform.agent;

import org.springframework.util.StringUtils;

/**
 * 将面向 Cursor 的 {@code agent} 命令行适配为 Claude Code 的 {@code claude} 调用约定。
 */
public final class AgentCliRewriter {

    private AgentCliRewriter() {}

    public static String adaptExecutable(String cmd, AgentCliKind kind) {
        if (!StringUtils.hasText(cmd) || kind != AgentCliKind.CLAUDE) {
            return cmd;
        }
        String c = cmd;
        // 先处理带 -f 的 Cursor 形式
        c = c.replace("agent --print -f ", "claude --print ");
        c = c.replace("agent --print -f\"", "claude --print \"");
        c = c.replace("agent --print -f'", "claude --print '");
        c = c.replace("agent --print ", "claude --print ");
        return c;
    }
}
