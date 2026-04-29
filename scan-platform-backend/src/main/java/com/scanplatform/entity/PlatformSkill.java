package com.scanplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 平台维护的 Cursor 技能：扫描前写入目标仓库 .cursor/skills/&lt;skill_name&gt;/SKILL.md，优先级高于仓库自带技能。
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

    @Column(name = "skill_body", nullable = false, columnDefinition = "LONGTEXT")
    private String skillBody;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
