package com.scanplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActiveScanJobDto {
    private Long id;
    @NotBlank
    private String jobName;
    @NotNull
    private Long repoId;
    private Integer scheduleEnabled;
    private String cronExpression;
    private String agentCommandOverride;
    private String scanSkillName;
    private String scanSkillPrompt;
    private Integer notifyOnFailure;
    private Integer notifyOnSuccess;
    private Integer status;
    /** 非空则覆盖仓库 display_commit */
    private Integer displayCommit;
}
