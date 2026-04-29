package com.scanplatform.repository;

import com.scanplatform.entity.ScanTaskLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ScanTaskLogRepository extends JpaRepository<ScanTaskLog, Long>, JpaSpecificationExecutor<ScanTaskLog> {
}
