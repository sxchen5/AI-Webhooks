package com.scanplatform.repository;

import com.scanplatform.entity.ActiveScanRepo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActiveScanRepoRepository extends JpaRepository<ActiveScanRepo, Long> {
}
