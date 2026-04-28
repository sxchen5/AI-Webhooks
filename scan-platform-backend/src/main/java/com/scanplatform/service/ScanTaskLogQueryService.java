package com.scanplatform.service;

import com.scanplatform.entity.ScanTaskLog;
import com.scanplatform.repository.ScanTaskLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 扫描日志分页与详情查询。
 */
@Service
@RequiredArgsConstructor
public class ScanTaskLogQueryService {

    private final ScanTaskLogRepository repository;

    @Transactional(readOnly = true)
    public Page<ScanTaskLog> page(Pageable pageable) {
        return repository.findAllByOrderByIdDesc(pageable);
    }

    @Transactional(readOnly = true)
    public ScanTaskLog get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("日志不存在"));
    }
}
