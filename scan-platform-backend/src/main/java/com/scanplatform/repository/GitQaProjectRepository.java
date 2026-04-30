package com.scanplatform.repository;

import com.scanplatform.entity.GitQaProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GitQaProjectRepository extends JpaRepository<GitQaProject, Long>, JpaSpecificationExecutor<GitQaProject> {

    @Query(
            "SELECT g FROM GitQaProject g WHERE "
                    + "LOWER(g.botName) LIKE LOWER(CONCAT('%', :kw, '%')) OR "
                    + "LOWER(g.gitUrl) LIKE LOWER(CONCAT('%', :kw, '%'))")
    Page<GitQaProject> pageByKeyword(@Param("kw") String kw, Pageable pageable);

    long countByGitProjectId(Long gitProjectId);
}
