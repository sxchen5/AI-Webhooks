package com.scanplatform.repository;

import com.scanplatform.entity.GitQaChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GitQaChatMessageRepository extends JpaRepository<GitQaChatMessage, Long> {

    Page<GitQaChatMessage> findByProjectIdOrderByIdAsc(Long projectId, Pageable pageable);

    void deleteByProjectId(Long projectId);

    Optional<GitQaChatMessage> findFirstByProjectIdAndIdGreaterThanOrderByIdAsc(Long projectId, Long id);
}
