-- 下发任务日志：是否展示 Commit（0 表示非单次提交类技能，列表/详情可隐藏 hash）
ALTER TABLE active_scan_log ADD COLUMN display_commit TINYINT DEFAULT 1 COMMENT '1展示 0不强调提交';

ALTER TABLE active_scan_repo ADD COLUMN display_commit TINYINT DEFAULT 1 COMMENT '1列表展示当前检出 commit';
ALTER TABLE active_scan_job ADD COLUMN display_commit TINYINT NULL COMMENT '非空则覆盖仓库';
