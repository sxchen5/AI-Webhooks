package com.scanplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 平台技能目录内的一条相对路径文件（UTF-8 文本） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSkillFileDto {

    /** 相对技能根目录的路径，使用正斜杠，如 {@code SKILL.md}、{@code scripts/check.sh} */
    @NotBlank
    private String path;

    /** 允许空文件体（占位）；SKILL.md 在服务端校验非空 */
    private String content;
}
