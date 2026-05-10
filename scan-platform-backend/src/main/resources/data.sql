-- 默认管理员（密码：admin123）；用户名冲突时忽略
INSERT INTO sys_user (username, password, status)
VALUES ('admin', '$2b$12$63sbOpUUedf08WWx7IiVneWDhTEOg1EsF2iOWiu5G2jk7mLWxaFBa', 1)
ON DUPLICATE KEY UPDATE username = username;

-- 保证至少一条邮件配置
INSERT INTO active_scan_mail (smtp_host, smtp_port, smtp_username, smtp_password, email_title_prefix, smtp_tls_enabled, smtp_ssl_enabled)
SELECT NULL, NULL, NULL, NULL, '【代码扫描通知】', 1, 0
FROM (SELECT 1 AS x) AS t
WHERE NOT EXISTS (SELECT 1 FROM active_scan_mail LIMIT 1);

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
