-- 已有库手动执行：Git 项目 AI 问答配置表

CREATE TABLE IF NOT EXISTS git_qa_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bot_name VARCHAR(128) NOT NULL COMMENT '问答机器人名称',
    git_project_id BIGINT COMMENT '关联 git_project',
    git_url VARCHAR(500) NOT NULL COMMENT 'Git 克隆地址',
    git_username VARCHAR(255) COMMENT 'HTTP(S) 用户名',
    git_password VARCHAR(500) COMMENT 'HTTP(S) 密码或 Token',
    branch VARCHAR(255) NOT NULL DEFAULT 'main',
    local_clone_path VARCHAR(500) COMMENT '本地克隆目录，空则使用 work-base-dir/git-qa-{id}',
    agent_cli VARCHAR(16) NOT NULL DEFAULT 'CURSOR' COMMENT 'CURSOR=agent，CLAUDE=claude',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_git_qa_git_project_id (git_project_id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT 'Git 项目 AI 问答配置';
