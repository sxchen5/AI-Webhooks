package com.scanplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 主动扫描执行日志。
 */
@Entity
@Table(name = "active_scan_log")
@Getter
@Setter
public class ActiveScanLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "repo_id")
    private Long repoId;

    @Column(name = "job_name", length = 255)
    private String jobName;

    @Column(name = "repo_name", length = 255)
    private String repoName;

    @Column(name = "trigger_type", length = 32)
    private String triggerType;

    @Column(name = "git_url", length = 500)
    private String gitUrl;

    @Column(length = 255)
    private String branch;

    @Column(name = "commit_hash", length = 255)
    private String commitHash;

    @Column(name = "clone_log", columnDefinition = "TEXT")
    private String cloneLog;

    @Column(name = "exec_command", columnDefinition = "TEXT")
    private String execCommand;

    @Column(name = "exec_result", columnDefinition = "LONGTEXT")
    private String execResult;

    @Column(name = "exec_status")
    private Integer execStatus;

    @Column(name = "email_status")
    private Integer emailStatus;

    @Column(name = "task_start_time")
    private LocalDateTime taskStartTime;

    @Column(name = "task_end_time")
    private LocalDateTime taskEndTime;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;
}
