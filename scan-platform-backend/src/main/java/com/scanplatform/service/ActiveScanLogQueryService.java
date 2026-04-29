package com.scanplatform.service;

import com.scanplatform.entity.ActiveScanLog;
import com.scanplatform.repository.ActiveScanLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActiveScanLogQueryService {

    private final ActiveScanLogRepository repository;

    @Transactional(readOnly = true)
    public Page<ActiveScanLog> page(Long repoId, String jobName, String repoName, Pageable pageable) {
        Specification<ActiveScanLog> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (repoId != null) {
                preds.add(cb.equal(root.get("repoId"), repoId));
            }
            if (StringUtils.hasText(jobName)) {
                preds.add(cb.like(root.get("jobName"), "%" + escapeLike(jobName.trim()) + "%", '\\'));
            }
            if (StringUtils.hasText(repoName)) {
                preds.add(cb.like(root.get("repoName"), "%" + escapeLike(repoName.trim()) + "%", '\\'));
            }
            if (preds.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public ActiveScanLog get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("日志不存在"));
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
