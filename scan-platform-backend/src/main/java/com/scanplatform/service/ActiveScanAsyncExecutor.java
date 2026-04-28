package com.scanplatform.service;

import com.scanplatform.entity.ActiveScanJob;
import com.scanplatform.entity.ActiveScanLog;
import com.scanplatform.entity.ActiveScanRepo;
import com.scanplatform.entity.SysConfig;
import com.scanplatform.repository.ActiveScanJobRepository;
import com.scanplatform.repository.ActiveScanLogRepository;
import com.scanplatform.repository.ActiveScanRepoRepository;
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
 * 主动扫描：git 同步后执行 agent 命令，写 active_scan_log，并按策略发邮件。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActiveScanAsyncExecutor {

    public static final String TRIGGER_MANUAL = "MANUAL";
    public static final String TRIGGER_SCHEDULE = "SCHEDULE";

    private final ActiveScanLogRepository logRepository;
    private final ActiveScanRepoRepository repoRepository;
    private final ActiveScanJobRepository jobRepository;
    private final GitWorkspaceService gitWorkspaceService;
    private final SysConfigService sysConfigService;
    private final ShellCommandService shellCommandService;
    private final AlertMailService alertMailService;

    @Async("scanTaskExecutor")
    public void executeAsync(Long activeLogId) {
        SysConfig config = sysConfigService.getEntity();
        ActiveScanLog task = logRepository.findById(activeLogId).orElse(null);
        if (task == null || task.getJobId() == null) {
            log.error("主动扫描日志不存在: {}", activeLogId);
            return;
        }
        ActiveScanJob job = jobRepository.findById(task.getJobId()).orElse(null);
        if (job == null || job.getStatus() == null || job.getStatus() != 1) {
            log.warn("主动扫描跳过: 任务不可用 logId={}", activeLogId);
            finishFailure(task, "任务不存在或已禁用", config);
            return;
        }
        ActiveScanRepo repo = repoRepository.findById(job.getRepoId()).orElse(null);
        if (repo == null || repo.getStatus() == null || repo.getStatus() != 1) {
            log.warn("主动扫描跳过: 仓库不可用 logId={} repoId={}", activeLogId, job.getRepoId());
            finishFailure(task, "仓库不存在或已禁用", config);
            return;
        }

        task.setTaskStartTime(LocalDateTime.now());
        logRepository.save(task);

        String workPath;
        String commit;
        StringBuilder cloneAggregate = new StringBuilder();
        try {
            String[] sync = gitWorkspaceService.ensureRepo(repo);
            workPath = sync[0];
            commit = sync[1];
            cloneAggregate.append(sync[2] != null ? sync[2] : "");
            task.setCommitHash(commit);
            task.setCloneLog(cloneAggregate.toString());
        } catch (Exception e) {
            log.error("主动扫描 git 同步失败 logId={}", activeLogId, e);
            task.setCloneLog(cloneAggregate + "\n" + e.getMessage());
            finishFailure(task, "Git 同步失败: " + e.getMessage(), config);
            notifyAfterRun(config, job, repo, task, false);
            return;
        }

        String template = StringUtils.hasText(job.getAgentCommandOverride())
                ? job.getAgentCommandOverride()
                : repo.getAgentCommand();
        String branch = StringUtils.hasText(repo.getBranch()) ? repo.getBranch() : "main";
        String cmd = ScanAsyncExecutor.buildCommand(template, workPath, branch, commit);
        task.setExecCommand(cmd);
        logRepository.save(task);
        log.info("主动扫描执行命令: logId={} job={}", activeLogId, job.getJobName());

        try {
            Map<String, String> env = activeScanEnv(repo, job, task, workPath, branch, commit);
            ShellCommandService.ShellResult result = shellCommandService.execute(cmd, Path.of(workPath), env);
            task.setExecResult(result.output());
            task.setTaskEndTime(LocalDateTime.now());
            boolean success = result.exitCode() == 0;
            task.setExecStatus(success ? 1 : 2);
            if (!success) {
                task.setEmailStatus(0);
            }
            logRepository.save(task);
            notifyAfterRun(config, job, repo, task, success);
            log.info("主动扫描结束: logId={} success={}", activeLogId, success);
        } catch (Exception e) {
            log.error("主动扫描执行异常 logId={}", activeLogId, e);
            finishFailure(task, e.getMessage(), config);
            notifyAfterRun(config, job, repo, task, false);
        }
    }

    private void finishFailure(ActiveScanLog task, String message, SysConfig config) {
        task.setExecResult(StringUtils.hasText(task.getExecResult()) ? task.getExecResult() + "\n" + message : message);
        task.setExecStatus(2);
        task.setTaskEndTime(LocalDateTime.now());
        logRepository.save(task);
    }

    /**
     * 按任务配置发送成功/失败邮件（Git 失败或执行失败 execStatus=2 视为失败）。
     */
    private void notifyAfterRun(SysConfig config, ActiveScanJob job, ActiveScanRepo repo, ActiveScanLog task, boolean execSuccess) {
        String emails = repo.getReceiveEmail();
        if (!StringUtils.hasText(emails)) {
            task.setEmailStatus(0);
            logRepository.save(task);
            return;
        }
        boolean wantNotify = execSuccess ? (job.getNotifyOnSuccess() == 1) : (job.getNotifyOnFailure() == 1);
        if (!wantNotify) {
            if (task.getEmailStatus() == null) {
                task.setEmailStatus(0);
            }
            logRepository.save(task);
            return;
        }
        String prefix = StringUtils.hasText(config.getEmailTitlePrefix()) ? config.getEmailTitlePrefix() : "【代码扫描通知】";
        String subject = prefix + (execSuccess ? " 主动扫描成功 - " : " 主动扫描失败 - ") + job.getJobName();
        String body = "任务: " + job.getJobName() + "\n仓库: " + repo.getRepoName() + "\n触发: " + task.getTriggerType()
                + "\n分支: " + task.getBranch() + "\nCommit: " + (task.getCommitHash() != null ? task.getCommitHash() : "")
                + "\n\nGit 日志:\n" + (task.getCloneLog() != null ? task.getCloneLog() : "")
                + "\n\n命令:\n" + (task.getExecCommand() != null ? task.getExecCommand() : "")
                + "\n\n输出:\n" + (task.getExecResult() != null ? task.getExecResult() : "");
        boolean sent = alertMailService.sendMail(config, emails, subject, body);
        task.setEmailStatus(sent ? 1 : 2);
        logRepository.save(task);
    }

    public static Map<String, String> activeScanEnv(ActiveScanRepo repo, ActiveScanJob job, ActiveScanLog task,
                                                    String path, String branch, String commit) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("ACTIVE_SCAN_REPO_PATH", path);
        m.put("ACTIVE_SCAN_BRANCH", branch);
        m.put("ACTIVE_SCAN_COMMIT", commit != null ? commit : "");
        m.put("ACTIVE_SCAN_JOB_ID", job.getId() != null ? String.valueOf(job.getId()) : "");
        m.put("ACTIVE_SCAN_REPO_ID", repo.getId() != null ? String.valueOf(repo.getId()) : "");
        m.put("ACTIVE_SCAN_JOB_NAME", job.getJobName() != null ? job.getJobName() : "");
        m.put("ACTIVE_SCAN_REPO_NAME", repo.getRepoName() != null ? repo.getRepoName() : "");
        m.put("ACTIVE_SCAN_TRIGGER", task.getTriggerType() != null ? task.getTriggerType() : "");
        // 与 WebHook 脚本兼容
        m.put("WEBHOOK_REPO_PATH", path);
        m.put("WEBHOOK_BRANCH", branch);
        m.put("WEBHOOK_COMMIT", commit != null ? commit : "");
        m.put("WEBHOOK_PROJECT_NAME", repo.getRepoName() != null ? repo.getRepoName() : "");
        m.put("WEBHOOK_GITLAB_PROJECT_ID", "");
        m.put("WEBHOOK_COMMIT_USER", "active-scan");
        m.put("WEBHOOK_GIT_URL", repo.getGitUrl() != null ? repo.getGitUrl() : "");
        return m;
    }
}
