-- 已有库：Agent CLI 类型 + 可配置模型表（在业务低峰期执行；列/表已存在可忽略报错）

CREATE TABLE IF NOT EXISTS agent_model_option (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cli_kind VARCHAR(16) NOT NULL COMMENT 'CURSOR 或 CLAUDE',
    model_key VARCHAR(64) NOT NULL,
    display_label VARCHAR(128),
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cli_model (cli_kind, model_key)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT 'Agent/Claude 模型白名单';

ALTER TABLE active_scan_repo
    ADD COLUMN agent_cli VARCHAR(16) NOT NULL DEFAULT 'CURSOR' COMMENT 'CURSOR 或 CLAUDE' AFTER agent_model;

ALTER TABLE active_scan_job
    ADD COLUMN agent_cli VARCHAR(16) NULL COMMENT '覆盖仓库 CLI' AFTER agent_model;

ALTER TABLE git_qa_project
    ADD COLUMN agent_cli VARCHAR(16) NOT NULL DEFAULT 'CURSOR' COMMENT 'CURSOR 或 CLAUDE' AFTER scan_skill_prompt;

INSERT IGNORE INTO agent_model_option (cli_kind, model_key, display_label, sort_order, status) VALUES
('CURSOR', 'auto', 'auto', 0, 1),
('CURSOR', 'composer-2-fast', 'composer-2-fast', 10, 1),
('CURSOR', 'composer-2', 'composer-2', 20, 1),
('CURSOR', 'composer-1.5', 'composer-1.5', 30, 1),
('CURSOR', 'grok-4-20', 'grok-4-20', 40, 1),
('CURSOR', 'grok-4-20-thinking', 'grok-4-20-thinking', 50, 1),
('CURSOR', 'kimi-k2.5', 'kimi-k2.5', 60, 1),
('CLAUDE', 'sonnet', 'Sonnet', 0, 1),
('CLAUDE', 'opus', 'Opus', 10, 1),
('CLAUDE', 'haiku', 'Haiku', 20, 1);
