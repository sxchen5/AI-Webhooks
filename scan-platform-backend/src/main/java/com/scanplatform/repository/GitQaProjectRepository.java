package com.scanplatform.repository;

import com.scanplatform.entity.GitQaProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GitQaProjectRepository extends JpaRepository<GitQaProject, Long>, JpaSpecificationExecutor<GitQaProject> {

    long countByGitProjectId(Long gitProjectId);
}
