package com.scanplatform.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Git 项目 AI 问答：克隆与 agent 配置（独立于主动扫描任务）。
 */
@Entity
@Table(name = "git_qa_project")
@Getter
@Setter
public class GitQaProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bot_name", nullable = false, length = 128)
    private String botName;

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

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
