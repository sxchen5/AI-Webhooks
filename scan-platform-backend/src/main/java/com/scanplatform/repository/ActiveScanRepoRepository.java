package com.scanplatform.repository;

import com.scanplatform.entity.ActiveScanRepo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActiveScanRepoRepository extends JpaRepository<ActiveScanRepo, Long>, JpaSpecificationExecutor<ActiveScanRepo> {

    long countByGitProjectId(Long gitProjectId);
}
