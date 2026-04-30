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
    smtp_host VARCHAR(255),
    smtp_port INT,
    smtp_username VARCHAR(255),
    smtp_password VARCHAR(255),
    email_title_prefix VARCHAR(255) DEFAULT '【代码扫描通知】',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '全局配置（邮件等）';

CREATE TABLE IF NOT EXISTS git_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_name VARCHAR(255) NOT NULL COMMENT '项目名称',
    git_url VARCHAR(500) NOT NULL COMMENT 'Git 项目地址',
    status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT 'Git项目管理';

CREATE TABLE IF NOT EXISTS active_scan_repo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    repo_name VARCHAR(255) NOT NULL COMMENT '项目名称（可与 git_project 同步）',
    git_project_id BIGINT COMMENT '关联 git_project',
    git_url VARCHAR(500) NOT NULL COMMENT 'Git 克隆地址',
    git_username VARCHAR(255) COMMENT 'HTTP(S) 用户名',
    git_password VARCHAR(500) COMMENT 'HTTP(S) 密码或 Token',
    branch VARCHAR(255) NOT NULL DEFAULT 'main',
    local_clone_path VARCHAR(500) COMMENT '本地克隆目录，空则自动生成',
    agent_command VARCHAR(1000) NOT NULL COMMENT '占位符 {{path}} {{branch}} {{commit}}；可与技能配置二选一',
    scan_skill_name VARCHAR(128),
    scan_skill_prompt TEXT,
    agent_model VARCHAR(64) COMMENT '可选，非空时在 agent 末尾追加 --model',
    receive_email VARCHAR(500) COMMENT '通知邮箱',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_git_project_id (git_project_id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '主动扫描-Git项目配置';

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
    agent_model VARCHAR(64) COMMENT '非空则覆盖仓库模型，在命令末尾追加 --model',
    notify_on_failure TINYINT DEFAULT 1,
    notify_on_success TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_repo_id (repo_id),
    KEY idx_next_run (next_schedule_run, schedule_enabled, status)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '主动扫描-任务';

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
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '主动扫描-执行日志';

CREATE TABLE IF NOT EXISTS platform_skill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    skill_name VARCHAR(128) NOT NULL COMMENT '与 .cursor/skills 目录名一致',
    description VARCHAR(500) COMMENT '说明',
    skill_body LONGTEXT NOT NULL COMMENT '完整 SKILL.md',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_skill_name (skill_name)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT '平台技能：扫描前写入工作区，优先级高于仓库 .cursor/skills';

CREATE TABLE IF NOT EXISTS git_qa_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bot_name VARCHAR(128) NOT NULL COMMENT '问答机器人名称',
    git_project_id BIGINT COMMENT '关联 git_project',
    git_url VARCHAR(500) NOT NULL COMMENT 'Git 克隆地址',
    git_username VARCHAR(255) COMMENT 'HTTP(S) 用户名',
    git_password VARCHAR(500) COMMENT 'HTTP(S) 密码或 Token',
    branch VARCHAR(255) NOT NULL DEFAULT 'main',
    local_clone_path VARCHAR(500) COMMENT '本地克隆目录，空则使用 work-base-dir/git-qa-{id}',
    agent_command VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '可选自定义 agent；空则对话使用 stream-json 默认命令',
    scan_skill_name VARCHAR(128),
    scan_skill_prompt TEXT,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_git_qa_git_project_id (git_project_id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT 'Git 项目 AI 问答配置';

CREATE TABLE IF NOT EXISTS git_qa_chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT 'git_qa_project.id',
    role VARCHAR(16) NOT NULL COMMENT 'USER 或 ASSISTANT',
    content LONGTEXT NOT NULL COMMENT '消息正文（Markdown/纯文本）',
    feedback TINYINT NULL COMMENT '助手消息反馈：1点赞 -1点踩',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_git_qa_chat_project_id (project_id),
    KEY idx_git_qa_chat_project_time (project_id, id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT 'Git 项目 AI 问答聊天记录';
