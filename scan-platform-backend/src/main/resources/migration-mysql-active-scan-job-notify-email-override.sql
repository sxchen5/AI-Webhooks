-- 下发任务：可选覆盖通知邮箱（多个逗号分隔）；新库已通过 schema.sql 包含时可忽略报错
ALTER TABLE active_scan_job
    ADD COLUMN notify_email_override VARCHAR(500) NULL COMMENT '非空则覆盖仓库 receive_email' AFTER notify_on_success;
