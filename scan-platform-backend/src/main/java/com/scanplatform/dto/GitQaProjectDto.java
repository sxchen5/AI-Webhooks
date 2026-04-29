package com.scanplatform.dto;

import jakarta.validation.constraints.AssertTrue;
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
    private String agentCommand;
    private String scanSkillName;
    private String scanSkillPrompt;
    private Integer status;

    @AssertTrue(message = "请填写「Agent 命令」或「扫描技能名」至少一项")
    public boolean isCommandOrSkill() {
        return (agentCommand != null && !agentCommand.isBlank())
                || (scanSkillName != null && !scanSkillName.isBlank());
    }
}
