package com.scanplatform.repository;

import com.scanplatform.entity.ScanTaskLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanTaskLogRepository extends JpaRepository<ScanTaskLog, Long> {

    Page<ScanTaskLog> findAllByOrderByIdDesc(Pageable pageable);
}
