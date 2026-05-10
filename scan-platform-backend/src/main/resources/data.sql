-- 默认管理员（密码：admin123）；用户名冲突时忽略
INSERT INTO sys_user (username, password, status)
VALUES ('admin', '$2b$12$63sbOpUUedf08WWx7IiVneWDhTEOg1EsF2iOWiu5G2jk7mLWxaFBa', 1)
ON DUPLICATE KEY UPDATE username = username;

-- 保证至少一条邮件配置
INSERT INTO active_scan_mail (smtp_host, smtp_port, smtp_username, smtp_password, email_title_prefix, smtp_tls_enabled, smtp_ssl_enabled)
SELECT NULL, NULL, NULL, NULL, '【代码扫描通知】', 1, 0
FROM (SELECT 1 AS x) AS t
WHERE NOT EXISTS (SELECT 1 FROM active_scan_mail LIMIT 1);
