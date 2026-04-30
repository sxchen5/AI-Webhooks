package com.scanplatform.service;

import com.scanplatform.entity.ActiveScanJob;
import com.scanplatform.entity.ActiveScanRepo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 当配置了 Cursor 技能名时，生成通过 {@code agent --print -f} 读取提示文件的命令；
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

        Path dir = workDirAbsolute.resolve(".scan-platform");
        Files.createDirectories(dir);
        String fileName = "scan-prompt-" + System.nanoTime() + ".txt";
        Path promptFile = dir.resolve(fileName).normalize();

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

        Files.writeString(promptFile, body.toString(), StandardCharsets.UTF_8);

        String promptPath = promptFile.toAbsolutePath().toString();
        if (isWindows()) {
            // cmd.exe 下双引号包裹路径
            return "agent --print -f \"" + escapeForCmdDoubleQuoted(promptPath) + "\"";
        }
        // bash: 单引号包裹路径，避免 $ 等展开
        return "agent --print -f '" + escapeForBashSingleQuoted(promptPath) + "'";
    }

    private static String sanitizeSkillSlug(String name) {
        if (!name.matches("^[a-zA-Z0-9][a-zA-Z0-9._-]*$")) {
            return "";
        }
        return name;
    }

    private static String escapeForCmdDoubleQuoted(String path) {
        return path.replace("\"", "\\\"");
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
     * Git 问答默认：问题写入临时文件后执行 {@code agent --print -f <file> --output-format stream-json}。
     */
    public String buildGitQaStreamJsonCommand(String userQuestion) throws IOException {
        Path dir = Path.of(System.getProperty("java.io.tmpdir", ".")).resolve("scan-platform-git-qa");
        Files.createDirectories(dir);
        Path promptFile = dir.resolve("git-qa-q-" + System.nanoTime() + ".txt").normalize();
        Files.writeString(promptFile, userQuestion != null ? userQuestion : "", StandardCharsets.UTF_8);
        String promptPath = promptFile.toAbsolutePath().toString();
        if (isWindows()) {
            return "agent --print -f \"" + escapeForCmdDoubleQuoted(promptPath) + "\" --output-format stream-json";
        }
        return "agent --print -f '" + escapeForBashSingleQuoted(promptPath) + "' --output-format stream-json";
    }
}
