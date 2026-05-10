-- 平台技能：支持多文件 JSON；skill_body 改为可空并由 SKILL.md 同步（在业务低峰期执行；列已存在可忽略）

ALTER TABLE platform_skill
    ADD COLUMN skill_files_json LONGTEXT NULL COMMENT 'JSON [{path,content}]' AFTER description;

ALTER TABLE platform_skill
    MODIFY COLUMN skill_body LONGTEXT NULL COMMENT '与 SKILL.md 同步，兼容旧读法';
