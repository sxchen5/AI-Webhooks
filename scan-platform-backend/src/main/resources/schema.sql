-- 严格按需求建表（首次启动由 Spring Boot 执行；若表已存在可设置 spring.sql.init.mode=never）

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL COMMENT '登录账号',
    password VARCHAR(64) NOT NULL COMMENT '登录密码',
    status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) COMMENT '系统用户表';

CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    webhook_token VARCHAR(255) COMMENT 'GitLab Webhook Token',
    gitlab_allow_ips VARCHAR(500) COMMENT '允许的GitLab IP，逗号分隔',
    smtp_host VARCHAR(255),
    smtp_port INT,
    smtp_username VARCHAR(255),
    smtp_password VARCHAR(255),
    email_title_prefix VARCHAR(255) DEFAULT '【代码扫描通知】',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '全局配置';

CREATE TABLE IF NOT EXISTS project_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    gitlab_project_id BIGINT NOT NULL COMMENT 'GitLab 项目ID',
    project_name VARCHAR(255) NOT NULL,
    git_url VARCHAR(500),
    local_code_path VARCHAR(500) NOT NULL COMMENT '本地代码目录',
    agent_command VARCHAR(1000) NOT NULL COMMENT '执行命令，支持占位符 {{path}} {{branch}} {{commit}}；若配置技能则可为占位由平台生成 agent 命令',
    scan_skill_name VARCHAR(128) COMMENT 'Cursor 技能名（.cursor/skills 下目录名），与 scan_skill_prompt 二选一或同时填则走 agent -f 提示文件',
    scan_skill_prompt TEXT COMMENT '扫描说明，与技能组合写入提示文件',
    receive_email VARCHAR(500) COMMENT '告警邮箱，多个逗号分隔',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_gitlab_project_id (gitlab_project_id)
) COMMENT '项目配置';

CREATE TABLE IF NOT EXISTS scan_task_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT,
    gitlab_project_id BIGINT,
    project_name VARCHAR(255),
    branch VARCHAR(255),
    commit_hash VARCHAR(255),
    commit_user VARCHAR(255),
    exec_command TEXT,
    exec_result LONGTEXT,
    exec_status TINYINT COMMENT '1成功 2失败',
    email_status TINYINT COMMENT '0未发送 1已发送 2发送失败',
    task_start_time DATETIME,
    task_end_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '扫描任务执行日志';

CREATE TABLE IF NOT EXISTS active_scan_repo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    repo_name VARCHAR(255) NOT NULL COMMENT '仓库显示名称',
    git_url VARCHAR(500) NOT NULL COMMENT 'Git 克隆地址',
    git_username VARCHAR(255) COMMENT 'HTTP(S) 用户名',
    git_password VARCHAR(500) COMMENT 'HTTP(S) 密码或 Token',
    branch VARCHAR(255) NOT NULL DEFAULT 'main',
    local_clone_path VARCHAR(500) COMMENT '本地克隆目录，空则自动生成',
    agent_command VARCHAR(1000) NOT NULL COMMENT '占位符 {{path}} {{branch}} {{commit}}；可与技能配置二选一',
    scan_skill_name VARCHAR(128),
    scan_skill_prompt TEXT,
    receive_email VARCHAR(500) COMMENT '通知邮箱',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '主动扫描-Git仓库';

CREATE TABLE IF NOT EXISTS active_scan_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_name VARCHAR(255) NOT NULL,
    repo_id BIGINT NOT NULL,
    schedule_enabled TINYINT DEFAULT 0,
    cron_expression VARCHAR(120),
    next_schedule_run DATETIME,
    last_schedule_run DATETIME,
    agent_command_override VARCHAR(1000),
    scan_skill_name VARCHAR(128) COMMENT '非空则覆盖仓库技能名',
    scan_skill_prompt TEXT COMMENT '非空则覆盖仓库技能补充说明',
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
    trigger_type VARCHAR(32),
    git_url VARCHAR(500),
    branch VARCHAR(255),
    commit_hash VARCHAR(255),
    clone_log TEXT,
    exec_command TEXT,
    exec_result LONGTEXT,
    exec_status TINYINT,
    email_status TINYINT,
    task_start_time DATETIME,
    task_end_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_job_id (job_id),
    KEY idx_create_time (create_time)
) COMMENT '主动扫描-执行日志';
