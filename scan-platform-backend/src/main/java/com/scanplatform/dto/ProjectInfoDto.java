package com.scanplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectInfoDto {

    private Long id;

    @NotNull
    private Long gitlabProjectId;

    @NotBlank
    private String projectName;

    private String gitUrl;

    @NotBlank
    private String localCodePath;

    @NotBlank
    private String agentCommand;

    private String receiveEmail;

    private Integer status;
}
