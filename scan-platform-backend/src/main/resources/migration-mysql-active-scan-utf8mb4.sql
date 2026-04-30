-- 已有库：主动扫描日志 exec_result/clone_log 等可能含 emoji（如 📄）；utf8mb3 会报 SQL 1366
ALTER TABLE active_scan_log CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE active_scan_repo CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE active_scan_job CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE platform_skill CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE git_qa_project CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
