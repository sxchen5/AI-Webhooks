package com.scanplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Git 项目管理：项目名称与仓库地址主数据。
 */
@Entity
@Table(name = "git_project")
@Getter
@Setter
public class GitProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_name", nullable = false, length = 255)
    private String projectName;

    @Column(name = "git_url", nullable = false, length = 500)
    private String gitUrl;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
