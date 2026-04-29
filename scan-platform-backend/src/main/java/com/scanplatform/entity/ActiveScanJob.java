package com.scanplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 主动扫描任务：关联仓库、可选定时 Cron、通知策略。
 */
@Entity
@Table(name = "active_scan_job")
@Getter
@Setter
public class ActiveScanJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 255)
    private String jobName;

    @Column(name = "repo_id", nullable = false)
    private Long repoId;

    @Column(name = "schedule_enabled", nullable = false)
    private Integer scheduleEnabled = 0;

    @Column(name = "cron_expression", length = 120)
    private String cronExpression;

    @Column(name = "next_schedule_run")
    private LocalDateTime nextScheduleRun;

    @Column(name = "last_schedule_run")
    private LocalDateTime lastScheduleRun;

    @Column(name = "agent_command_override", length = 1000)
    private String agentCommandOverride;

    @Column(name = "scan_skill_name", length = 128)
    private String scanSkillName;

    @Column(name = "scan_skill_prompt", columnDefinition = "TEXT")
    private String scanSkillPrompt;

    /** 非空则覆盖仓库 display_commit */
    @Column(name = "display_commit")
    private Integer displayCommit;

    @Column(name = "notify_on_failure", nullable = false)
    private Integer notifyOnFailure = 1;

    @Column(name = "notify_on_success", nullable = false)
    private Integer notifyOnSuccess = 0;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
