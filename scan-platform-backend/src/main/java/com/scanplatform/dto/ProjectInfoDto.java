package com.scanplatform.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectInfoDto {

    private Long id;

    @NotNull
    private Long gitlabProjectId;

    @NotBlank
    private String projectName;

    private String gitUrl;

    @NotBlank
    private String localCodePath;

    /** 与 scanSkillName 二选一：至少填一项；填技能时可为占位命令 */
    private String agentCommand;

    /** Cursor 技能目录名，非空则使用 agent -f 提示文件并 /name 触发技能 */
    private String scanSkillName;

    private String scanSkillPrompt;

    private String receiveEmail;

    private Integer status;

    @AssertTrue(message = "请填写「Agent 命令」或「扫描技能名」至少一项")
    public boolean isCommandOrSkill() {
        return (agentCommand != null && !agentCommand.isBlank())
                || (scanSkillName != null && !scanSkillName.isBlank());
    }
}
