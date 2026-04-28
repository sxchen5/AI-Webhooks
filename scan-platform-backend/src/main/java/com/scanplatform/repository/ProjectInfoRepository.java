package com.scanplatform.repository;

import com.scanplatform.entity.ProjectInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectInfoRepository extends JpaRepository<ProjectInfo, Long> {

    Optional<ProjectInfo> findByGitlabProjectId(Long gitlabProjectId);
}
