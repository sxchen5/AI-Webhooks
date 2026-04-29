-- 为已有库增加「Cursor 技能扫描」可选字段（新库已通过 schema.sql 包含下列列时可忽略报错）

ALTER TABLE project_info
    ADD COLUMN scan_skill_name VARCHAR(128) NULL COMMENT 'Cursor 技能名，对应 .cursor/skills/<name>/',
    ADD COLUMN scan_skill_prompt TEXT NULL COMMENT '与技能一并传给 agent -p 的补充说明';

ALTER TABLE active_scan_repo
    ADD COLUMN scan_skill_name VARCHAR(128) NULL,
    ADD COLUMN scan_skill_prompt TEXT NULL;

ALTER TABLE active_scan_job
    ADD COLUMN scan_skill_name VARCHAR(128) NULL COMMENT '非空则覆盖仓库技能配置',
    ADD COLUMN scan_skill_prompt TEXT NULL;
