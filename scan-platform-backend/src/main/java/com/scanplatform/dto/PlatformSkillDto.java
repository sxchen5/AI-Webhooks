package com.scanplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlatformSkillDto {
    private Long id;
    @NotBlank
    private String skillName;
    private String description;
    @NotBlank
    private String skillBody;
    private Integer status;
}
