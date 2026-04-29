package com.scanplatform.repository;

import com.scanplatform.entity.ActiveScanLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActiveScanLogRepository extends JpaRepository<ActiveScanLog, Long>, JpaSpecificationExecutor<ActiveScanLog> {

    Page<ActiveScanLog> findAllByOrderByIdDesc(Pageable pageable);
}
