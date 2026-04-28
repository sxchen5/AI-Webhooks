-- 主动扫描（定时 / 手动），与 WebHook 扫描并存；首次部署若已有库可单独执行本文件

CREATE TABLE IF NOT EXISTS active_scan_repo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    repo_name VARCHAR(255) NOT NULL COMMENT '仓库显示名称',
    git_url VARCHAR(500) NOT NULL COMMENT 'Git 克隆地址 https/ssh',
    git_username VARCHAR(255) COMMENT 'HTTP(S) 克隆用户名，可选',
    git_password VARCHAR(500) COMMENT 'HTTP(S) 克隆密码或 Token，明文存储请注意库权限',
    branch VARCHAR(255) NOT NULL DEFAULT 'main' COMMENT '检出分支',
    local_clone_path VARCHAR(500) COMMENT '本地克隆目录，空则使用系统工作区自动生成',
    agent_command VARCHAR(1000) NOT NULL COMMENT 'agent 命令，占位符 {{path}} {{branch}} {{commit}}',
    receive_email VARCHAR(500) COMMENT '通知邮箱，逗号分隔',
    status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '主动扫描-Git仓库';

CREATE TABLE IF NOT EXISTS active_scan_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_name VARCHAR(255) NOT NULL,
    repo_id BIGINT NOT NULL COMMENT '关联 active_scan_repo',
    schedule_enabled TINYINT DEFAULT 0 COMMENT '1启用定时',
    cron_expression VARCHAR(120) COMMENT 'Spring 6 位 cron，如 0 0 2 * * ?',
    next_schedule_run DATETIME COMMENT '下次定时触发时间',
    last_schedule_run DATETIME,
    agent_command_override VARCHAR(1000) COMMENT '非空则覆盖仓库默认命令',
    notify_on_failure TINYINT DEFAULT 1,
    notify_on_success TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_repo_id (repo_id),
    KEY idx_next_run (next_schedule_run, schedule_enabled, status)
) COMMENT '主动扫描-任务';

CREATE TABLE IF NOT EXISTS active_scan_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id BIGINT,
    repo_id BIGINT,
    job_name VARCHAR(255),
    repo_name VARCHAR(255),
    trigger_type VARCHAR(32) COMMENT 'MANUAL SCHEDULE',
    git_url VARCHAR(500),
    branch VARCHAR(255),
    commit_hash VARCHAR(255),
    clone_log TEXT COMMENT 'git clone/fetch 输出',
    exec_command TEXT,
    exec_result LONGTEXT,
    exec_status TINYINT COMMENT '1成功 2失败',
    email_status TINYINT COMMENT '0未发送 1已发送 2发送失败',
    task_start_time DATETIME,
    task_end_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_job_id (job_id),
    KEY idx_create_time (create_time)
) COMMENT '主动扫描-执行日志';
