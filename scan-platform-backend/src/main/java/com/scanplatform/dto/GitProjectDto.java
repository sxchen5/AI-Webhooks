package com.scanplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GitProjectDto {
    private Long id;
    @NotBlank
    private String projectName;
    @NotBlank
    private String gitUrl;
    @NotNull
    private Integer status;
}
