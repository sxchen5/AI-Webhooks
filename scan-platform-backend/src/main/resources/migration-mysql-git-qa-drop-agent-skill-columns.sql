-- Git 问答表精简：删除 agent_command / scan_skill_*（对话仅 stream-json + agent_cli 组装）
-- 请在备份后执行；若列已删除，对应行报错可忽略

ALTER TABLE git_qa_project DROP COLUMN scan_skill_prompt;
ALTER TABLE git_qa_project DROP COLUMN scan_skill_name;
ALTER TABLE git_qa_project DROP COLUMN agent_command;
