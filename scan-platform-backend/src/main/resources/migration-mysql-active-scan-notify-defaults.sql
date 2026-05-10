-- 主动扫描任务：历史数据中 notify_on_* 为 NULL 时与实体默认值对齐，避免不发失败邮件
UPDATE active_scan_job SET notify_on_failure = 1 WHERE notify_on_failure IS NULL;
UPDATE active_scan_job SET notify_on_success = 0 WHERE notify_on_success IS NULL;
