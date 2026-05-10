-- 从已部署库移除 WebHook 相关表与邮件配置表中的 WebHook 列（在业务低峰期手工执行；列已删除时 ALTER 会报错可忽略）
-- 邮件配置表当前名为 active_scan_mail；若环境仍为旧表名 sys_config，请将下列 ALTER 中的表名替换为 sys_config。

DROP TABLE IF EXISTS scan_task_log;
DROP TABLE IF EXISTS project_info;

ALTER TABLE active_scan_mail DROP COLUMN webhook_token;
ALTER TABLE active_scan_mail DROP COLUMN gitlab_allow_ips;
