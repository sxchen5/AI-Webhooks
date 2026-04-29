-- 从已部署库移除 WebHook 相关表与 sys_config 中的 WebHook 列（在业务低峰期手工执行；列已删除时 ALTER 会报错可忽略）

DROP TABLE IF EXISTS scan_task_log;
DROP TABLE IF EXISTS project_info;

ALTER TABLE sys_config DROP COLUMN webhook_token;
ALTER TABLE sys_config DROP COLUMN gitlab_allow_ips;
