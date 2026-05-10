package com.scanplatform.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActiveScanRepoDto {
    private Long id;
    @NotBlank
    private String repoName;
    /** 可选：关联 git_project */
    private Long gitProjectId;
    @NotBlank
    private String gitUrl;
    private String gitUsername;
    /** 前端传新密码；空且为编辑时不修改 */
    private String gitPassword;
    private String branch;
    private String localClonePath;
    private String agentCommand;
    private String scanSkillName;
    private String scanSkillPrompt;
    /** 可选：非空则在命令末尾追加 --model（白名单见 Agent 模型配置） */
    private String agentModel;
    /** CURSOR 或 CLAUDE */
    private String agentCli;
    private String receiveEmail;
    private Integer status;

    @AssertTrue(message = "请填写「Agent 命令」或「扫描技能名」至少一项")
    public boolean isCommandOrSkill() {
        return (agentCommand != null && !agentCommand.isBlank())
                || (scanSkillName != null && !scanSkillName.isBlank());
    }
}
