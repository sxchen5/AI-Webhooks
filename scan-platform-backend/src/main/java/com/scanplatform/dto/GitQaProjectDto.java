package com.scanplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GitQaProjectDto {
    private Long id;
    @NotBlank
    private String botName;
    private Long gitProjectId;
    @NotBlank
    private String gitUrl;
    private String gitUsername;
    private String gitPassword;
    private String branch;
    private String localClonePath;
    /** CURSOR 或 CLAUDE */
    private String agentCli;
    private Integer status;
}
