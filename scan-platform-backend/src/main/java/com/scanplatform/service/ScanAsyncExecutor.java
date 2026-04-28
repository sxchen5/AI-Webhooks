package com.scanplatform.service;

import com.scanplatform.entity.ProjectInfo;
import com.scanplatform.entity.ScanTaskLog;
import com.scanplatform.entity.SysConfig;
import com.scanplatform.repository.ProjectInfoRepository;
import com.scanplatform.repository.ScanTaskLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 异步执行扫描命令、更新日志、失败时发邮件。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScanAsyncExecutor {

    private final ScanTaskLogRepository logRepository;
    private final ProjectInfoRepository projectRepository;
    private final SysConfigService sysConfigService;
    private final ShellCommandService shellCommandService;
    private final AlertMailService alertMailService;

    @Async("scanTaskExecutor")
    public void executeAsync(Long scanLogId) {
        SysConfig config = sysConfigService.getEntity();
        ScanTaskLog task = logRepository.findById(scanLogId).orElse(null);
        if (task == null || task.getProjectId() == null) {
            log.error("扫描任务不存在或缺少 projectId: {}", scanLogId);
            return;
        }
        ProjectInfo project = projectRepository.findById(task.getProjectId()).orElse(null);
        if (project == null || project.getStatus() == null || project.getStatus() != 1) {
            finishFailure(task, "项目不存在或已禁用", config);
            return;
        }
        String cmd = buildCommand(project.getAgentCommand(), project.getLocalCodePath(), task.getBranch(), task.getCommitHash());
        task.setExecCommand(cmd);
        task.setTaskStartTime(LocalDateTime.now());
        logRepository.save(task);

        try {
            Map<String, String> env = webhookEnv(project, task);
            ShellCommandService.ShellResult result = shellCommandService.execute(cmd, Path.of(project.getLocalCodePath()), env);
            task.setExecResult(result.output());
            task.setTaskEndTime(LocalDateTime.now());
            if (result.exitCode() == 0) {
                task.setExecStatus(1);
                task.setEmailStatus(0);
            } else {
                task.setExecStatus(2);
                trySendAlert(config, project, task, "扫描命令退出码: " + result.exitCode());
            }
            logRepository.save(task);
        } catch (Exception e) {
            log.error("扫描执行异常 logId={}", scanLogId, e);
            finishFailure(task, e.getMessage(), config);
        }
    }

    private void finishFailure(ScanTaskLog task, String message, SysConfig config) {
        task.setExecResult(StringUtils.hasText(task.getExecResult()) ? task.getExecResult() + "\n" + message : message);
        task.setExecStatus(2);
        task.setTaskEndTime(LocalDateTime.now());
        logRepository.save(task);
        ProjectInfo project = projectRepository.findById(task.getProjectId()).orElse(null);
        if (project != null && config != null) {
            trySendAlert(config, project, task, message);
        }
    }

    private void trySendAlert(SysConfig config, ProjectInfo project, ScanTaskLog task, String reason) {
        String emails = project.getReceiveEmail();
        if (!StringUtils.hasText(emails)) {
            task.setEmailStatus(0);
            logRepository.save(task);
            return;
        }
        String prefix = StringUtils.hasText(config.getEmailTitlePrefix()) ? config.getEmailTitlePrefix() : "【代码扫描通知】";
        String subject = prefix + " 扫描失败 - " + project.getProjectName();
        String body = "项目: " + project.getProjectName() + " (GitLab ID: " + project.getGitlabProjectId() + ")\n"
                + "分支: " + task.getBranch() + "\n"
                + "Commit: " + task.getCommitHash() + "\n"
                + "提交人: " + task.getCommitUser() + "\n"
                + "原因: " + reason + "\n\n"
                + "执行命令:\n" + task.getExecCommand() + "\n\n"
                + "输出:\n" + (task.getExecResult() != null ? task.getExecResult() : "");
        boolean sent = alertMailService.sendFailureAlert(config, emails, subject, body);
        task.setEmailStatus(sent ? 1 : 2);
        logRepository.save(task);
    }

    /**
     * WebHook 扫描子进程环境变量，供自定义脚本或 Cursor 包装脚本读取（与 {{path}} 等占位符互补）。
     */
    public static Map<String, String> webhookEnv(ProjectInfo project, ScanTaskLog task) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("WEBHOOK_REPO_PATH", project.getLocalCodePath() != null ? project.getLocalCodePath() : "");
        m.put("WEBHOOK_BRANCH", task.getBranch() != null ? task.getBranch() : "");
        m.put("WEBHOOK_COMMIT", task.getCommitHash() != null ? task.getCommitHash() : "");
        m.put("WEBHOOK_PROJECT_NAME", project.getProjectName() != null ? project.getProjectName() : "");
        m.put("WEBHOOK_GITLAB_PROJECT_ID", project.getGitlabProjectId() != null ? String.valueOf(project.getGitlabProjectId()) : "");
        m.put("WEBHOOK_COMMIT_USER", task.getCommitUser() != null ? task.getCommitUser() : "");
        m.put("WEBHOOK_GIT_URL", project.getGitUrl() != null ? project.getGitUrl() : "");
        return m;
    }

    /**
     * 将 agent_command 中的占位符替换为实际值。
     */
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
