package com.scanplatform.repository;

import com.scanplatform.entity.ProjectInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProjectInfoRepository extends JpaRepository<ProjectInfo, Long>, JpaSpecificationExecutor<ProjectInfo> {

    Optional<ProjectInfo> findByGitlabProjectId(Long gitlabProjectId);
}
