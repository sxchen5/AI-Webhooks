package com.scanplatform.service;

import com.scanplatform.entity.ActiveScanJob;
import com.scanplatform.repository.ActiveScanJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 每分钟检查到期的定时主动扫描任务。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActiveScanSchedulePoller {

    private final ActiveScanJobRepository jobRepository;
    private final ActiveScanCronHelper cronHelper;
    private final ActiveScanTriggerService triggerService;

    @Scheduled(cron = "0 * * * * ?")
    public void pollDueJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<ActiveScanJob> due = jobRepository.findByScheduleEnabledAndStatusAndNextScheduleRunLessThanEqual(1, 1, now);
        for (ActiveScanJob job : due) {
            if (!StringUtils.hasText(job.getCronExpression())) {
                job.setNextScheduleRun(null);
                jobRepository.save(job);
                continue;
            }
            try {
                triggerService.triggerScheduled(job.getId());
                job.setLastScheduleRun(now);
                LocalDateTime next = cronHelper.nextFireTime(job.getCronExpression(), now);
                job.setNextScheduleRun(next);
                jobRepository.save(job);
                log.debug("定时主动扫描已派发: jobId={} next={}", job.getId(), next);
            } catch (Exception e) {
                log.error("定时主动扫描派发失败 jobId={}", job.getId(), e);
            }
        }
    }
}
