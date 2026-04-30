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
    /** 可选：覆盖仓库的模型名，非空则在 agent 命令末尾追加 --model */
    private String agentModel;
    private Integer notifyOnFailure;
    private Integer notifyOnSuccess;
    private Integer status;
}
