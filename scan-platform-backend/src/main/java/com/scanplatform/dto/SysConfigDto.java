package com.scanplatform.dto;

import lombok.Data;

/**
 * 主动扫描邮件配置 DTO（不落库敏感字段时可按需裁剪）。
 */
@Data
public class SysConfigDto {
    private Long id;
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    /** 前端编辑时传新密码；为空表示不修改 */
    private String smtpPassword;
    private String emailTitlePrefix;
    /** 1 启用 STARTTLS */
    private Integer smtpTlsEnabled;
    /** 1 启用 SSL（如 465） */
    private Integer smtpSslEnabled;
}
