package com.scanplatform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class PlatformSkillDto {
    private Long id;
    @NotBlank
    private String skillName;
    private String description;
    /** 多文件技能目录；与 {@link #skillBody} 二选一（同时提交时以本字段为准） */
    @Valid
    private List<PlatformSkillFileDto> files;
    /** 兼容旧客户端：仅单文件 SKILL.md */
    private String skillBody;
    private Integer status;
}
