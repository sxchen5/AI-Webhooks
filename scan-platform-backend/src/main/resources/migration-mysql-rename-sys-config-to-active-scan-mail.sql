-- 邮件配置表重命名与 TLS/SSL 列（在业务低峰期手工执行）
--
-- 若数据库中仍存在表 sys_config、且尚未有 active_scan_mail，请先单独执行：
--   RENAME TABLE sys_config TO active_scan_mail;
--
-- 下列语句在表名为 active_scan_mail 时执行；列已存在时报错可忽略。

ALTER TABLE active_scan_mail
    ADD COLUMN smtp_tls_enabled TINYINT NOT NULL DEFAULT 1 COMMENT 'STARTTLS' AFTER email_title_prefix;

ALTER TABLE active_scan_mail
    ADD COLUMN smtp_ssl_enabled TINYINT NOT NULL DEFAULT 0 COMMENT 'SSL 465' AFTER smtp_tls_enabled;
