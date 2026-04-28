package com.scanplatform.repository;

import com.scanplatform.entity.ActiveScanJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ActiveScanJobRepository extends JpaRepository<ActiveScanJob, Long> {

    List<ActiveScanJob> findByScheduleEnabledAndStatusAndNextScheduleRunLessThanEqual(
            Integer scheduleEnabled, Integer status, LocalDateTime before);

    long countByRepoId(Long repoId);
}
