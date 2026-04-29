package com.scanplatform.service;

import com.scanplatform.entity.ScanTaskLog;
import com.scanplatform.repository.ScanTaskLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScanTaskLogQueryService {

    private final ScanTaskLogRepository repository;

    @Transactional(readOnly = true)
    public Page<ScanTaskLog> page(Long projectId, Pageable pageable) {
        Specification<ScanTaskLog> spec = (root, q, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (projectId != null) {
                preds.add(cb.equal(root.get("projectId"), projectId));
            }
            if (preds.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public ScanTaskLog get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("日志不存在"));
    }
}