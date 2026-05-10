package com.scanplatform.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PlatformSkillDetailDto {
    private Long id;
    private String skillName;
    private String description;
    private List<PlatformSkillFileDto> files;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
