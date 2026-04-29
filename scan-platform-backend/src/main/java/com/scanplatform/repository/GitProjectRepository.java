package com.scanplatform.repository;

import com.scanplatform.entity.GitProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GitProjectRepository extends JpaRepository<GitProject, Long>, JpaSpecificationExecutor<GitProject> {
}
