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
    agent_command VARCHAR(1000) NOT NULL COMMENT '执行命令，支持占位符 {{path}} {{branch}} {{commit}}',
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
