package com.scanplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 平台技能：扫描前按 Agent CLI 写入目标仓库 {@code .cursor/skills/&lt;skill_name&gt;/} 或 {@code .claude/skills/&lt;skill_name&gt;/}（至少含 SKILL.md）。
 */
@Entity
@Table(name = "platform_skill")
@Getter
@Setter
public class PlatformSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "skill_name", nullable = false, length = 128, unique = true)
    private String skillName;

    @Column(length = 500)
    private String description;

    /** JSON 数组 [{path,content}]，相对技能包根目录（.cursor/skills/&lt;skillName&gt;/ 或 .claude/skills/&lt;skillName&gt;/） */
    @Column(name = "skill_files_json", columnDefinition = "LONGTEXT")
    private String skillFilesJson;

    /** 与 {@link #skillFilesJson} 中 SKILL.md 正文同步，兼容旧逻辑 */
    @Column(name = "skill_body", columnDefinition = "LONGTEXT")
    private String skillBody;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
