-- 已有库升级：Git 项目管理与 Git 项目配置关联（手工执行）

CREATE TABLE IF NOT EXISTS git_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_name VARCHAR(255) NOT NULL COMMENT '项目名称',
    git_url VARCHAR(500) NOT NULL COMMENT 'Git 项目地址',
    status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT 'Git项目管理';

ALTER TABLE active_scan_repo ADD COLUMN git_project_id BIGINT NULL COMMENT '关联 git_project' AFTER repo_name;
ALTER TABLE active_scan_repo ADD KEY idx_git_project_id (git_project_id);
