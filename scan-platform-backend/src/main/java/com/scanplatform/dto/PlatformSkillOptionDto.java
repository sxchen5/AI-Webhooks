package com.scanplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 下拉用：启用的平台技能名称与说明 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSkillOptionDto {
    private String skillName;
    private String description;
}
