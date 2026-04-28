package com.scanplatform.service;

import com.scanplatform.entity.ActiveScanJob;
import com.scanplatform.entity.ActiveScanLog;
import com.scanplatform.entity.ActiveScanRepo;
import com.scanplatform.repository.ActiveScanJobRepository;
import com.scanplatform.repository.ActiveScanLogRepository;
import com.scanplatform.repository.ActiveScanRepoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActiveScanTriggerService {

    private final ActiveScanJobRepository jobRepository;
    private final ActiveScanRepoRepository repoRepository;
    private final ActiveScanLogRepository logRepository;
    private final ActiveScanAsyncExecutor asyncExecutor;

    @Transactional
    public Long triggerManual(Long jobId) {
        return start(jobId, ActiveScanAsyncExecutor.TRIGGER_MANUAL);
    }

    @Transactional
    public Long triggerScheduled(Long jobId) {
        return start(jobId, ActiveScanAsyncExecutor.TRIGGER_SCHEDULE);
    }

    private Long start(Long jobId, String triggerType) {
        ActiveScanJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        if (job.getStatus() == null || job.getStatus() != 1) {
            throw new IllegalArgumentException("任务已禁用");
        }
        ActiveScanRepo repo = repoRepository.findById(job.getRepoId())
                .orElseThrow(() -> new IllegalArgumentException("仓库不存在"));
        if (repo.getStatus() == null || repo.getStatus() != 1) {
            throw new IllegalArgumentException("仓库已禁用");
        }

        ActiveScanLog row = new ActiveScanLog();
        row.setJobId(job.getId());
        row.setRepoId(repo.getId());
        row.setJobName(job.getJobName());
        row.setRepoName(repo.getRepoName());
        row.setTriggerType(triggerType);
        row.setGitUrl(repo.getGitUrl());
        row.setBranch(repo.getBranch());
        row.setEmailStatus(0);
        row = logRepository.save(row);

        asyncExecutor.executeAsync(row.getId());
        log.info("主动扫描已触发: logId={} jobId={} trigger={}", row.getId(), jobId, triggerType);
        return row.getId();
    }
}
