package com.scanplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 全局配置 sys_config：SMTP 与邮件标题前缀等。
 */
@Entity
@Table(name = "sys_config")
@Getter
@Setter
public class SysConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "smtp_host", length = 255)
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_username", length = 255)
    private String smtpUsername;

    @Column(name = "smtp_password", length = 255)
    private String smtpPassword;

    @Column(name = "email_title_prefix", length = 255)
    private String emailTitlePrefix = "【代码扫描通知】";

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
