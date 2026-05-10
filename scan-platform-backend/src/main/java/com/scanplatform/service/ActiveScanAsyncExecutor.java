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
    private final AgentCommandBuilder agentCommandBuilder;

    @Async("scanTaskExecutor")
    public void executeAsync(Long activeLogId) {
        log.info("主动扫描异步开始: logId={}", activeLogId);
        SysConfig config = sysConfigService.getEntity();
        ActiveScanLog task = logRepository.findById(activeLogId).orElse(null);
        if (task == null || task.getJobId() == null) {
            log.error("主动扫描日志不存在: {}", activeLogId);
            return;
        }
        ActiveScanJob job = jobRepository.findById(task.getJobId()).orElse(null);
        if (job == null || job.getStatus() == null || job.getStatus() != 1) {
            log.warn("主动扫描跳过: 任务不可用 logId={} jobId={}", activeLogId, task.getJobId());
            finishFailure(task, "任务不存在或已禁用");
            return;
        }
        ActiveScanRepo repo = repoRepository.findById(job.getRepoId()).orElse(null);
        if (repo == null || repo.getStatus() == null || repo.getStatus() != 1) {
            log.warn("主动扫描跳过: 仓库不可用 logId={} repoId={}", activeLogId, job.getRepoId());
            finishFailure(task, "仓库不存在或已禁用");
            return;
        }

        log.info(
                "主动扫描上下文: logId={} jobId={} jobName={} repoId={} repoName={} branch={} notifyOnSuccess={} notifyOnFailure={} notifyEmailOverride={} repoReceiveEmail={}",
                activeLogId,
                job.getId(),
                job.getJobName(),
                repo.getId(),
                repo.getRepoName(),
                repo.getBranch(),
                job.getNotifyOnSuccess(),
                job.getNotifyOnFailure(),
                StringUtils.hasText(job.getNotifyEmailOverride()) ? "(任务已覆盖)" : "(未覆盖)",
                StringUtils.hasText(repo.getReceiveEmail()) ? "(已配置)" : "(未配置)");

        task.setTaskStartTime(LocalDateTime.now());
        logRepository.save(task);

        String workPath;
        String commit;
        StringBuilder cloneAggregate = new StringBuilder();
        try {
            log.info("主动扫描开始 Git 同步: logId={} gitUrl={}", activeLogId, repo.getGitUrl());
            String[] sync = gitWorkspaceService.ensureRepo(repo);
            workPath = sync[0];
            commit = sync[1];
            cloneAggregate.append(sync[2] != null ? sync[2] : "");
            task.setCommitHash(commit);
            task.setCloneLog(cloneAggregate.toString());
            log.info(
                    "主动扫描 Git 同步完成: logId={} workPath={} commit={} cloneLogChars={}",
                    activeLogId,
                    workPath,
                    commit,
                    task.getCloneLog() != null ? task.getCloneLog().length() : 0);
        } catch (Exception e) {
            log.error("主动扫描 git 同步失败 logId={}", activeLogId, e);
            task.setCloneLog(cloneAggregate + "\n" + e.getMessage());
            finishFailure(task, "Git 同步失败: " + e.getMessage());
            notifyAfterRun(config, job, repo, task, false);
            return;
        }

        String branch = StringUtils.hasText(repo.getBranch()) ? repo.getBranch() : "main";
        String cmd;
        try {
            cmd = agentCommandBuilder.resolveActiveScanCommand(repo, job, workPath, branch, commit);
        } catch (Exception e) {
            log.error("主动扫描构建命令失败 logId={}", activeLogId, e);
            finishFailure(task, "构建扫描命令失败: " + e.getMessage());
            notifyAfterRun(config, job, repo, task, false);
            return;
        }
        task.setExecCommand(cmd);
        logRepository.save(task);
        log.info("主动扫描准备执行命令: logId={} job={} branch={} cmdChars={}", activeLogId, job.getJobName(), branch, cmd.length());

        try {
            Map<String, String> env = activeScanEnv(repo, job, task, workPath, branch, commit);
            log.info("主动扫描开始 shell: logId={} cwd={} envKeys={}", activeLogId, workPath, env.size());
            ShellCommandService.ShellResult result = shellCommandService.execute(cmd, Path.of(workPath), env);
            String out = result.output();
            task.setExecResult(out);
            task.setTaskEndTime(LocalDateTime.now());
            boolean success = result.exitCode() == 0;
            task.setExecStatus(success ? 1 : 2);
            log.info(
                    "主动扫描 shell 结束: logId={} exitCode={} success={} outputChars={}",
                    activeLogId,
                    result.exitCode(),
                    success,
                    out != null ? out.length() : 0);
            logRepository.save(task);
            notifyAfterRun(config, job, repo, task, success);
            log.info("主动扫描全流程结束: logId={} emailStatus={}", activeLogId, task.getEmailStatus());
        } catch (Exception e) {
            log.error("主动扫描执行异常 logId={}", activeLogId, e);
            finishFailure(task, e.getMessage());
            notifyAfterRun(config, job, repo, task, false);
        }
    }

    private void finishFailure(ActiveScanLog task, String message) {
        task.setExecResult(StringUtils.hasText(task.getExecResult()) ? task.getExecResult() + "\n" + message : message);
        task.setExecStatus(2);
        task.setTaskEndTime(LocalDateTime.now());
        logRepository.save(task);
        log.warn("主动扫描标记失败: logId={} message={}", task.getId(), message);
    }

    /**
     * 按任务配置发送成功/失败邮件（Git 失败或执行失败 execStatus=2 视为失败）。
     * 通知邮箱：任务 notify_email_override 非空时优先，否则 Git 仓库 receive_email；SMTP 发件方取自 active_scan_mail。
     * notify_on_* 在库中为 NULL 时：失败通知默认开启(1)，成功通知默认关闭(0)，与实体默认值一致。
     */
    private void notifyAfterRun(SysConfig config, ActiveScanJob job, ActiveScanRepo repo, ActiveScanLog task, boolean execSuccess) {
        int notifyFail = job.getNotifyOnFailure() != null ? job.getNotifyOnFailure() : 1;
        int notifyOk = job.getNotifyOnSuccess() != null ? job.getNotifyOnSuccess() : 0;
        boolean wantNotify = execSuccess ? (notifyOk == 1) : (notifyFail == 1);

        log.info(
                "主动扫描邮件策略: logId={} execSuccess={} notifyOnSuccess={} notifyOnFailure={} resolvedWantNotify={}",
                task.getId(),
                execSuccess,
                notifyOk,
                notifyFail,
                wantNotify);

        String emails = StringUtils.hasText(job.getNotifyEmailOverride()) ? job.getNotifyEmailOverride().trim() : repo.getReceiveEmail();
        if (!StringUtils.hasText(emails)) {
            task.setEmailStatus(0);
            logRepository.save(task);
            log.warn(
                    "主动扫描跳过发邮件: logId={} repoId={} 原因=未配置收件人（任务未填覆盖邮箱且 Git 项目 receive_email 为空）",
                    task.getId(),
                    repo.getId());
            return;
        }
        if (!wantNotify) {
            if (task.getEmailStatus() == null) {
                task.setEmailStatus(0);
            }
            logRepository.save(task);
            log.info(
                    "主动扫描跳过发邮件: logId={} 原因=任务未开启{}通知(notify_on_{}={})",
                    task.getId(),
                    execSuccess ? "成功" : "失败",
                    execSuccess ? "success" : "failure",
                    execSuccess ? notifyOk : notifyFail);
            return;
        }
        if (!StringUtils.hasText(config.getSmtpHost()) || config.getSmtpPort() == null) {
            task.setEmailStatus(2);
            logRepository.save(task);
            log.warn(
                    "主动扫描邮件未发送: logId={} 原因=系统邮件配置中未配置 SMTP 主机或端口(smtp_host/smtp_port)",
                    task.getId());
            return;
        }
        String prefix = StringUtils.hasText(config.getEmailTitlePrefix()) ? config.getEmailTitlePrefix() : "【代码扫描通知】";
        String subject = prefix + (execSuccess ? " 主动扫描成功 - " : " 主动扫描失败 - ") + job.getJobName();
        String body = "任务: " + job.getJobName() + "\n仓库: " + repo.getRepoName() + "\n触发: " + task.getTriggerType()
                + "\n分支: " + task.getBranch()
                + "\nCommit: " + (task.getCommitHash() != null ? task.getCommitHash() : "")
                + "\n\nGit 日志:\n" + (task.getCloneLog() != null ? task.getCloneLog() : "")
                + "\n\n命令:\n" + (task.getExecCommand() != null ? task.getExecCommand() : "")
                + "\n\n输出:\n" + (task.getExecResult() != null ? task.getExecResult() : "");
        boolean sent = alertMailService.sendMail(config, emails, subject, body);
        task.setEmailStatus(sent ? 1 : 2);
        logRepository.save(task);
        log.info("主动扫描邮件结果: logId={} sent={} emailStatus={}", task.getId(), sent, task.getEmailStatus());
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
        // 与自定义脚本中常见 WEBHOOK_* 环境变量命名兼容
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
