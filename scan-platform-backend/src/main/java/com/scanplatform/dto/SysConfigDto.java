package com.scanplatform.dto;

import lombok.Data;

/**
 * 全局配置读写 DTO（不落库敏感字段时可按需裁剪）。
 */
@Data
public class SysConfigDto {
    private Long id;
    private String webhookToken;
    private String gitlabAllowIps;
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    /** 前端编辑时传新密码；为空表示不修改 */
    private String smtpPassword;
    private String emailTitlePrefix;
}
