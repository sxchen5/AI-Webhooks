package com.scanplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 主动扫描邮件与 SMTP 配置（表 active_scan_mail，原 sys_config）。
 */
@Entity
@Table(name = "active_scan_mail")
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

    /** 1 使用 STARTTLS（常见 587）；与 smtp_ssl_enabled 一般二选一 */
    @Column(name = "smtp_tls_enabled", nullable = false)
    private Integer smtpTlsEnabled = 1;

    /** 1 使用 SSL 隐式加密（常见 465）；为 1 时通常应关闭 TLS */
    @Column(name = "smtp_ssl_enabled", nullable = false)
    private Integer smtpSslEnabled = 0;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
