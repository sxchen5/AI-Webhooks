package com.scanplatform.service;

import com.scanplatform.agent.AgentCliKind;
import com.scanplatform.agent.AgentCliRewriter;
import com.scanplatform.entity.ActiveScanJob;
import com.scanplatform.entity.ActiveScanRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 根据 {@link AgentCliKind} 生成 Cursor {@code agent} 或 Claude Code {@code claude} 的 {@code --print} 行内命令；
 * 平台技能按 CLI 写入 {@code .cursor/skills/...}（Cursor）或 {@code .claude/skills/...}（Claude Code）。
 */
@Component
@RequiredArgsConstructor
public class AgentCommandBuilder {

    private final PlatformSkillMaterializer platformSkillMaterializer;
    private final AgentModelCatalogService agentModelCatalogService;

    public String buildSkillAgentCommand(
            Path workDirAbsolute,
            String branch,
            String commit,
            String contextLabel,
            String skillFolderName,
            String skillPrompt,
            AgentCliKind cli) throws IOException {
        return buildSkillAgentCommand(workDirAbsolute, branch, commit, contextLabel, skillFolderName, skillPrompt, null, cli);
    }

    public String buildSkillAgentCommand(
            Path workDirAbsolute,
            String branch,
            String commit,
            String contextLabel,
            String skillFolderName,
            String skillPrompt,
            String userQuestion,
            AgentCliKind cli) throws IOException {

        if (!StringUtils.hasText(skillFolderName)) {
            return null;
        }
        String slug = sanitizeSkillSlug(skillFolderName.trim());
        if (!StringUtils.hasText(slug)) {
            throw new IllegalArgumentException(
                    "扫描技能名无效，请使用字母数字与连字符（与 .cursor/skills 或 .claude/skills 下目录名一致，随 Agent CLI）");
        }

        platformSkillMaterializer.materializeIfPresent(workDirAbsolute, slug, cli);

        String p = workDirAbsolute.toString();
        String b = branch != null ? branch : "";
        String c = commit != null ? commit : "";
        String q = userQuestion != null ? userQuestion : "";
        String label = StringUtils.hasText(contextLabel) ? contextLabel : "repo";

        StringBuilder body = new StringBuilder();
        body.append("/").append(slug).append(" ");
        body.append("请按上述技能执行本次任务。上下文：").append(label);
        body.append("；工作目录: ").append(p).append("。\n");
        if (StringUtils.hasText(skillPrompt)) {
            String extra = skillPrompt
                    .replace("{{path}}", p)
                    .replace("{{branch}}", b)
                    .replace("{{commit}}", c)
                    .replace("{{question}}", q);
            body.append(extra.trim()).append("\n");
        }
        if (StringUtils.hasText(userQuestion)) {
            body.append("\n【用户问题】\n").append(userQuestion.trim()).append("\n");
        }

        return wrapPrintInline(cli, body.toString());
    }

    private static String wrapPrintInline(AgentCliKind cli, String prompt) {
        if (cli == AgentCliKind.CLAUDE) {
            if (isWindows()) {
                String oneLine = prompt.replace('\r', ' ').replace('\n', ' ').trim();
                return "claude --print \"" + escapeForCmdDoubleQuoted(oneLine) + "\"";
            }
            return "claude --print '" + escapeForBashSingleQuoted(prompt) + "'";
        }
        if (isWindows()) {
            String oneLine = prompt.replace('\r', ' ').replace('\n', ' ').trim();
            return "agent --print -f \"" + escapeForCmdDoubleQuoted(oneLine) + "\"";
        }
        return "agent --print -f '" + escapeForBashSingleQuoted(prompt) + "'";
    }

    private static String sanitizeSkillSlug(String name) {
        if (!name.matches("^[a-zA-Z0-9][a-zA-Z0-9._-]*$")) {
            return "";
        }
        return name;
    }

    private static String escapeForCmdDoubleQuoted(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String escapeForBashSingleQuoted(String path) {
        return path.replace("'", "'\"'\"'");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * 主动扫描：任务层技能字段优先于仓库；CLI 与模型按任务/仓库继承。
     * 输出为普通终端结果（{@code agent --print -f} / {@code claude --print}），不追加
     * {@code --output-format stream-json}；流式 JSON 仅用于 Git 问答 {@link #buildGitQaStreamJsonCommand}。
     */
    public String resolveActiveScanCommand(ActiveScanRepo repo, ActiveScanJob job,
                                           String workPath, String branch, String commit) throws IOException {
        AgentCliKind cli = resolveCliForJob(job, repo);
        Path dir = Path.of(workPath).toAbsolutePath().normalize();
        String skill = StringUtils.hasText(job.getScanSkillName())
                ? job.getScanSkillName().trim()
                : (StringUtils.hasText(repo.getScanSkillName()) ? repo.getScanSkillName().trim() : null);
        String prompt = StringUtils.hasText(job.getScanSkillPrompt())
                ? job.getScanSkillPrompt()
                : repo.getScanSkillPrompt();
        String cmd;
        if (StringUtils.hasText(skill)) {
            cmd = buildSkillAgentCommand(dir, branch, commit, repo.getRepoName(), skill, prompt, null, cli);
        } else {
            String template = StringUtils.hasText(job.getAgentCommandOverride())
                    ? job.getAgentCommandOverride()
                    : repo.getAgentCommand();
            cmd = AgentCommandUtil.buildCommand(template, dir.toString(), branch, commit);
        }
        cmd = AgentCliRewriter.adaptExecutable(cmd, cli);
        String model = StringUtils.hasText(job.getAgentModel()) ? job.getAgentModel() : repo.getAgentModel();
        return agentModelCatalogService.appendModelFlag(cli, cmd, model);
    }

    private static AgentCliKind resolveCliForJob(ActiveScanJob job, ActiveScanRepo repo) {
        if (StringUtils.hasText(job.getAgentCli())) {
            return AgentCliKind.fromDb(job.getAgentCli());
        }
        return AgentCliKind.fromDb(repo.getAgentCli());
    }

    private static String appendStreamJsonIfAbsent(AgentCliKind cli, String cmd) {
        if (cmd == null) {
            return null;
        }
        if (cmd.contains("--output-format")) {
            if (cli == AgentCliKind.CLAUDE && !cmd.contains("--verbose")) {
                return cmd + " --verbose";
            }
            return cmd;
        }
        if (cli == AgentCliKind.CLAUDE) {
            return cmd + " --output-format stream-json --verbose";
        }
        return cmd + " --output-format stream-json";
    }

    /** Git 问答默认：行内问题 + stream-json（Claude 追加 --verbose） */
    public String buildGitQaStreamJsonCommand(String userQuestion, AgentCliKind cli) {
        String q = userQuestion != null ? userQuestion : "";
        return appendStreamJsonIfAbsent(cli, wrapPrintInline(cli, q));
    }
}
