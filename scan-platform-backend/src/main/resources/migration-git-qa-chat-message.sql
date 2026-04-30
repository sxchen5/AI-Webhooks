-- 已有库：Git 问答聊天记录
CREATE TABLE IF NOT EXISTS git_qa_chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT 'git_qa_project.id',
    role VARCHAR(16) NOT NULL COMMENT 'USER 或 ASSISTANT',
    content LONGTEXT NOT NULL COMMENT '消息正文（Markdown/纯文本）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_git_qa_chat_project_id (project_id),
    KEY idx_git_qa_chat_project_time (project_id, id)
) COMMENT 'Git 项目 AI 问答聊天记录';
