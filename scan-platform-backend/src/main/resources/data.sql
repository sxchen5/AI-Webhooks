-- 默认管理员（密码：admin123）；用户名冲突时忽略
INSERT INTO sys_user (username, password, status)
VALUES ('admin', '$2b$12$63sbOpUUedf08WWx7IiVneWDhTEOg1EsF2iOWiu5G2jk7mLWxaFBa', 1)
ON DUPLICATE KEY UPDATE username = username;

-- 保证至少一条全局配置
INSERT INTO sys_config (webhook_token, gitlab_allow_ips, smtp_host, smtp_port, smtp_username, smtp_password, email_title_prefix)
SELECT NULL, NULL, NULL, NULL, NULL, NULL, '【代码扫描通知】'
FROM (SELECT 1 AS x) AS t
WHERE NOT EXISTS (SELECT 1 FROM sys_config LIMIT 1);
