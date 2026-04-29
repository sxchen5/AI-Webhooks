-- 已有库：删除 display_commit 列（MySQL；列不存在时报错可忽略该行）

ALTER TABLE active_scan_log DROP COLUMN display_commit;
ALTER TABLE active_scan_repo DROP COLUMN display_commit;
ALTER TABLE active_scan_job DROP COLUMN display_commit;
