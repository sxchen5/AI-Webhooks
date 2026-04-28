package com.scanplatform.repository;

import com.scanplatform.entity.ActiveScanLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActiveScanLogRepository extends JpaRepository<ActiveScanLog, Long> {

    Page<ActiveScanLog> findAllByOrderByIdDesc(Pageable pageable);
}
