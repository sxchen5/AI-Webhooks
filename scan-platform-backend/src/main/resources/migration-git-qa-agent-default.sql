-- 已有库：Git 问答 agent_command 允许为空（默认走 stream-json 问答命令）
ALTER TABLE git_qa_project MODIFY COLUMN agent_command VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '可选自定义 agent；空则对话使用 stream-json 默认命令';
