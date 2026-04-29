package com.scanplatform.service;

import com.scanplatform.dto.GitProjectDto;
import com.scanplatform.entity.GitProject;
import com.scanplatform.repository.ActiveScanRepoRepository;
import com.scanplatform.repository.GitProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitProjectService {

    private final GitProjectRepository repository;
    private final ActiveScanRepoRepository activeScanRepoRepository;

    @Transactional(readOnly = true)
    public Page<GitProject> page(String projectName, Pageable pageable) {
        if (!StringUtils.hasText(projectName)) {
            return repository.findAll(pageable);
        }
        String n = projectName.trim();
        return repository.findAll((root, q, cb) -> cb.like(root.get("projectName"), "%" + escapeLike(n) + "%", '\\'), pageable);
    }

    @Transactional(readOnly = true)
    public GitProject get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Git 项目不存在"));
    }

    @Transactional(readOnly = true)
    public List<GitProject> listForOptions() {
        return repository.findAll(
                org.springframework.data.domain.Sort.by("projectName")).stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .toList();
    }

    @Transactional
    public GitProject create(GitProjectDto dto) {
        GitProject e = new GitProject();
        apply(dto, e);
        return repository.save(e);
    }

    @Transactional
    public GitProject update(Long id, GitProjectDto dto) {
        GitProject e = get(id);
        apply(dto, e);
        return repository.save(e);
    }

    @Transactional
    public void delete(Long id) {
        if (activeScanRepoRepository.countByGitProjectId(id) > 0) {
            throw new IllegalArgumentException("该 Git 项目仍被「Git 项目配置」引用，请先解除关联或删除相关配置");
        }
        repository.deleteById(id);
        log.info("删除 Git 项目: id={}", id);
    }

    private void apply(GitProjectDto dto, GitProject e) {
        e.setProjectName(dto.getProjectName().trim());
        e.setGitUrl(dto.getGitUrl().trim());
        e.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
