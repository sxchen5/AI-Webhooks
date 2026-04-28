package com.scanplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActiveScanRepoDto {
    private Long id;
    @NotBlank
    private String repoName;
    @NotBlank
    private String gitUrl;
    private String gitUsername;
    /** 前端传新密码；空且为编辑时不修改 */
    private String gitPassword;
    private String branch;
    private String localClonePath;
    @NotBlank
    private String agentCommand;
    private String receiveEmail;
    private Integer status;
}
