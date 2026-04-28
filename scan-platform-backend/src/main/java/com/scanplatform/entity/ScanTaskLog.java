package com.scanplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 扫描任务执行日志 scan_task_log。
 */
@Entity
@Table(name = "scan_task_log")
@Getter
@Setter
public class ScanTaskLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "gitlab_project_id")
    private Long gitlabProjectId;

    @Column(name = "project_name", length = 255)
    private String projectName;

    @Column(length = 255)
    private String branch;

    @Column(name = "commit_hash", length = 255)
    private String commitHash;

    @Column(name = "commit_user", length = 255)
    private String commitUser;

    @Column(name = "exec_command", columnDefinition = "TEXT")
    private String execCommand;

    @Column(name = "exec_result", columnDefinition = "LONGTEXT")
    private String execResult;

    /** 1 成功 2 失败 */
    @Column(name = "exec_status")
    private Integer execStatus;

    /** 0 未发送 1 已发送 2 发送失败 */
    @Column(name = "email_status")
    private Integer emailStatus;

    @Column(name = "task_start_time")
    private LocalDateTime taskStartTime;

    @Column(name = "task_end_time")
    private LocalDateTime taskEndTime;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;
}
