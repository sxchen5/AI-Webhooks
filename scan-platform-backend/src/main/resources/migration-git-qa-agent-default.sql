-- 已废弃：旧版 git_qa_project 曾含 agent_command 列，本脚本用于放宽默认值。
-- 当前版本已删除 agent_command / scan_skill_name / scan_skill_prompt，新环境无需执行。
-- 从旧表升级请执行 migration-mysql-git-qa-drop-agent-skill-columns.sql。
SELECT 1;
