-- 已有库增加平台技能表（新库已通过 schema.sql 包含时可忽略报错）

CREATE TABLE IF NOT EXISTS platform_skill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    skill_name VARCHAR(128) NOT NULL COMMENT '技能目录名；执行时写入 .cursor/skills 或 .claude/skills（随 Agent CLI），扫描时 /name 触发',
    description VARCHAR(500) COMMENT '列表说明',
    skill_body LONGTEXT NOT NULL COMMENT '完整 SKILL.md 正文（含 YAML frontmatter）',
    status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_skill_name (skill_name)
) COMMENT '平台技能，扫描前按 Agent CLI 写入 .cursor/skills 或 .claude/skills 覆盖同名目录';
