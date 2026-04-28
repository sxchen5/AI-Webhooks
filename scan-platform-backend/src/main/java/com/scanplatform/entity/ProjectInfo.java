package com.scanplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 项目配置 project_info：与 GitLab 项目 ID 绑定，定义本地路径与扫描命令。
 */
@Entity
@Table(name = "project_info")
@Getter
@Setter
public class ProjectInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gitlab_project_id", nullable = false, unique = true)
    private Long gitlabProjectId;

    @Column(name = "project_name", nullable = false, length = 255)
    private String projectName;

    @Column(name = "git_url", length = 500)
    private String gitUrl;

    @Column(name = "local_code_path", nullable = false, length = 500)
    private String localCodePath;

    @Column(name = "agent_command", nullable = false, length = 1000)
    private String agentCommand;

    @Column(name = "receive_email", length = 500)
    private String receiveEmail;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
