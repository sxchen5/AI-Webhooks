package com.scanplatform.service;

import com.scanplatform.entity.ActiveScanJob;
import com.scanplatform.entity.ActiveScanRepo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 当配置了 Cursor 技能名时，生成 {@code agent --print -p} 行内提示命令（不再写入工作区 txt）；
 * 提示首行使用 {@code /技能目录名} 显式触发技能（与 Cursor 文档一致）。
 * 未配置技能时返回 empty，由调用方使用原始 {@code agent_command} 模板。
 */
@Component
public class AgentCommandBuilder {

    private final PlatformSkillMaterializer platformSkillMaterializer;

    public AgentCommandBuilder(PlatformSkillMaterializer platformSkillMaterializer) {
        this.platformSkillMaterializer = platformSkillMaterializer;
    }

    /**
     * @param workDirAbsolute  代码根目录（已解析占位符后的绝对路径）
     * @param branch           分支
     * @param commit           commit
     * @param contextLabel     项目/仓库显示名，写入提示便于模型理解
     * @param skillFolderName  .cursor/skills/&lt;name&gt;/ 中的 name，用于 /name 触发
     * @param skillPrompt      补充说明（可含 {{path}} 等占位符）
     * @return 若 skillFolderName 为空则返回 null
     */
    public String buildSkillAgentCommand(
            Path workDirAbsolute,
            String branch,
            String commit,
            String contextLabel,
            String skillFolderName,
            String skillPrompt) throws IOException {
        return buildSkillAgentCommand(workDirAbsolute, branch, commit, contextLabel, skillFolderName, skillPrompt, null);
    }

    /**
     * @param userQuestion 非空时追加到提示末尾，供 Git 问答等场景将用户问题一并交给 agent
     */
    public String buildSkillAgentCommand(
            Path workDirAbsolute,
            String branch,
            String commit,
            String contextLabel,
            String skillFolderName,
            String skillPrompt,
            String userQuestion) throws IOException {

        if (!StringUtils.hasText(skillFolderName)) {
            return null;
        }
        String slug = sanitizeSkillSlug(skillFolderName.trim());
        if (!StringUtils.hasText(slug)) {
            throw new IllegalArgumentException("扫描技能名无效，请使用字母数字与连字符（与 .cursor/skills 下目录名一致）");
        }

        // 平台技能优先：存在则写入工作区 .cursor/skills/<slug>/SKILL.md，覆盖仓库同名技能
        platformSkillMaterializer.materializeIfPresent(workDirAbsolute, slug);

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

        return wrapAgentPrintInline(body.toString());
    }

    /**
     * {@code agent --print -p} 行内提示；Windows cmd 下换行会破坏引号参数，故压成空格。
     */
    private static String wrapAgentPrintInline(String prompt) {
        if (isWindows()) {
            String oneLine = prompt.replace('\r', ' ').replace('\n', ' ').trim();
            return "agent --print -p \"" + escapeForCmdDoubleQuoted(oneLine) + "\"";
        }
        return "agent --print -p '" + escapeForBashSingleQuoted(prompt) + "'";
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
        // 单引号内不能再有单引号，用 '\'' 技巧
        return path.replace("'", "'\"'\"'");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /** 主动扫描：任务层技能字段优先于仓库 */
    public String resolveActiveScanCommand(ActiveScanRepo repo, ActiveScanJob job,
                                             String workPath, String branch, String commit) throws IOException {
        Path dir = Path.of(workPath).toAbsolutePath().normalize();
        String skill = StringUtils.hasText(job.getScanSkillName())
                ? job.getScanSkillName().trim()
                : (StringUtils.hasText(repo.getScanSkillName()) ? repo.getScanSkillName().trim() : null);
        String prompt = StringUtils.hasText(job.getScanSkillPrompt())
                ? job.getScanSkillPrompt()
                : repo.getScanSkillPrompt();
        if (StringUtils.hasText(skill)) {
            return buildSkillAgentCommand(dir, branch, commit, repo.getRepoName(), skill, prompt, null);
        }
        String template = StringUtils.hasText(job.getAgentCommandOverride())
                ? job.getAgentCommandOverride()
                : repo.getAgentCommand();
        return AgentCommandUtil.buildCommand(template, dir.toString(), branch, commit);
    }

    /** Git 项目 AI 问答：无任务层覆盖，支持将用户问题写入技能提示或 {{question}} 占位符 */
    public String resolveGitQaCommand(com.scanplatform.entity.GitQaProject qp,
                                     String workPath, String branch, String commit, String userQuestion) throws IOException {
        Path dir = Path.of(workPath).toAbsolutePath().normalize();
        String skill = StringUtils.hasText(qp.getScanSkillName()) ? qp.getScanSkillName().trim() : null;
        String prompt = qp.getScanSkillPrompt();
        String label = StringUtils.hasText(qp.getBotName()) ? qp.getBotName().trim() : "Git问答";
        if (StringUtils.hasText(skill)) {
            return appendStreamJsonIfAbsent(buildSkillAgentCommand(dir, branch, commit, label, skill, prompt, userQuestion));
        }
        String template = qp.getAgentCommand();
        return appendStreamJsonIfAbsent(
                AgentCommandUtil.buildCommand(template, dir.toString(), branch, commit, userQuestion != null ? userQuestion : ""));
    }

    private static String appendStreamJsonIfAbsent(String cmd) {
        if (cmd == null) {
            return null;
        }
        if (cmd.contains("--output-format")) {
            return cmd;
        }
        return cmd + " --output-format stream-json";
    }

    /**
     * Git 问答默认：用户问题通过 {@code agent --print -p} 行内传入，并追加 {@code stream-json}。
     */
    public String buildGitQaStreamJsonCommand(String userQuestion) {
        String q = userQuestion != null ? userQuestion : "";
        return wrapAgentPrintInline(q) + " --output-format stream-json";
    }
}
