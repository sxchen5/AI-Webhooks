package com.scanplatform.service;

import com.scanplatform.dto.ActiveScanJobDto;
import com.scanplatform.entity.ActiveScanJob;
import com.scanplatform.repository.ActiveScanJobRepository;
import com.scanplatform.repository.ActiveScanRepoRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActiveScanJobService {

    private final ActiveScanJobRepository jobRepository;
    private final ActiveScanRepoRepository repoRepository;
    private final ActiveScanCronHelper cronHelper;

    @Transactional(readOnly = true)
    public Page<ActiveScanJob> page(String jobName, Long repoId, Pageable pageable) {
        Specification<ActiveScanJob> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (repoId != null) {
                preds.add(cb.equal(root.get("repoId"), repoId));
            }
            if (StringUtils.hasText(jobName)) {
                String j = escapeLike(jobName.trim());
                preds.add(cb.like(root.get("jobName"), "%" + j + "%", '\\'));
            }
            if (preds.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        return jobRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public ActiveScanJob get(Long id) {
        return jobRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("任务不存在"));
    }

    @Transactional
    public ActiveScanJob create(ActiveScanJobDto dto) {
        repoRepository.findById(dto.getRepoId()).orElseThrow(() -> new IllegalArgumentException("仓库不存在"));
        ActiveScanJob j = new ActiveScanJob();
        apply(dto, j);
        computeNextRun(j);
        return jobRepository.save(j);
    }

    @Transactional
    public ActiveScanJob update(Long id, ActiveScanJobDto dto) {
        ActiveScanJob j = get(id);
        repoRepository.findById(dto.getRepoId()).orElseThrow(() -> new IllegalArgumentException("仓库不存在"));
        apply(dto, j);
        computeNextRun(j);
        return jobRepository.save(j);
    }

    @Transactional
    public void delete(Long id) {
        jobRepository.deleteById(id);
        log.info("删除主动扫描任务: id={}", id);
    }

    private void apply(ActiveScanJobDto dto, ActiveScanJob j) {
        j.setJobName(dto.getJobName());
        j.setRepoId(dto.getRepoId());
        j.setScheduleEnabled(dto.getScheduleEnabled() != null && dto.getScheduleEnabled() == 1 ? 1 : 0);
        j.setCronExpression(StringUtils.hasText(dto.getCronExpression()) ? dto.getCronExpression().trim() : null);
        j.setAgentCommandOverride(StringUtils.hasText(dto.getAgentCommandOverride()) ? dto.getAgentCommandOverride() : null);
        j.setScanSkillName(StringUtils.hasText(dto.getScanSkillName()) ? dto.getScanSkillName().trim() : null);
        j.setScanSkillPrompt(StringUtils.hasText(dto.getScanSkillPrompt()) ? dto.getScanSkillPrompt().trim() : null);
        j.setAgentModel(GitQaModelSupport.normalizeOrNull(dto.getAgentModel()));
        j.setNotifyOnFailure(dto.getNotifyOnFailure() != null ? dto.getNotifyOnFailure() : 1);
        j.setNotifyOnSuccess(dto.getNotifyOnSuccess() != null ? dto.getNotifyOnSuccess() : 0);
        j.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
    }

    private void computeNextRun(ActiveScanJob j) {
        if (j.getScheduleEnabled() != null && j.getScheduleEnabled() == 1 && StringUtils.hasText(j.getCronExpression())) {
            LocalDateTime next = cronHelper.nextFireTime(j.getCronExpression(), LocalDateTime.now());
            j.setNextScheduleRun(next);
        } else {
            j.setNextScheduleRun(null);
        }
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
