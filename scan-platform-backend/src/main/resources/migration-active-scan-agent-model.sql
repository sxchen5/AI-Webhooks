-- 主动扫描：可选 agent 模型（与 Git 问答白名单一致，非空时在命令末尾追加 --model）
ALTER TABLE active_scan_repo ADD COLUMN agent_model VARCHAR(64) NULL COMMENT '可选，非空时在 agent 末尾追加 --model';
ALTER TABLE active_scan_job ADD COLUMN agent_model VARCHAR(64) NULL COMMENT '非空则覆盖仓库模型，在命令末尾追加 --model';
