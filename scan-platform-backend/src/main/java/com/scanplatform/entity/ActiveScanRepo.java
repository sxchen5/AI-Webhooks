package com.scanplatform.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 主动扫描：Git 仓库配置（多仓库、凭据、分支、agent 命令）。
 */
@Entity
@Table(name = "active_scan_repo")
@Getter
@Setter
public class ActiveScanRepo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_name", nullable = false, length = 255)
    private String repoName;

    /** 可选：关联 git_project，用于从主数据带出 Git URL */
    @Column(name = "git_project_id")
    private Long gitProjectId;

    @Column(name = "git_url", nullable = false, length = 500)
    private String gitUrl;

    @Column(name = "git_username", length = 255)
    private String gitUsername;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "git_password", length = 500)
    private String gitPassword;

    @Column(nullable = false, length = 255)
    private String branch = "main";

    @Column(name = "local_clone_path", length = 500)
    private String localClonePath;

    @Column(name = "agent_command", nullable = false, length = 1000)
    private String agentCommand;

    @Column(name = "scan_skill_name", length = 128)
    private String scanSkillName;

    @Column(name = "scan_skill_prompt", columnDefinition = "TEXT")
    private String scanSkillPrompt;

    @Column(name = "receive_email", length = 500)
    private String receiveEmail;

    /** 1 列表/日志展示当前检出 commit；0 不强调单次提交（技能为全仓扫描时） */
    @Column(name = "display_commit", nullable = false)
    private Integer displayCommit = 1;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
