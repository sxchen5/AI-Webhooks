package com.scanplatform.service;

import com.scanplatform.dto.ProjectInfoDto;
import com.scanplatform.entity.ProjectInfo;
import com.scanplatform.repository.ProjectInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 项目配置 CRUD，按 gitlab_project_id 唯一约束。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectInfoService {

    private final ProjectInfoRepository repository;

    @Transactional(readOnly = true)
    public Page<ProjectInfo> page(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public ProjectInfo get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("项目不存在"));
    }

    @Transactional(readOnly = true)
    public ProjectInfo findByGitlabProjectId(Long gitlabProjectId) {
        return repository.findByGitlabProjectId(gitlabProjectId).orElse(null);
    }

    @Transactional
    public ProjectInfo create(ProjectInfoDto dto) {
        if (repository.findByGitlabProjectId(dto.getGitlabProjectId()).isPresent()) {
            throw new IllegalArgumentException("GitLab 项目ID已存在");
        }
        ProjectInfo p = new ProjectInfo();
        apply(dto, p);
        ProjectInfo saved = repository.save(p);
        log.info("创建项目配置: id={} gitlabProjectId={} name={}", saved.getId(), saved.getGitlabProjectId(), saved.getProjectName());
        return saved;
    }

    @Transactional
    public ProjectInfo update(Long id, ProjectInfoDto dto) {
        ProjectInfo p = get(id);
        if (!p.getGitlabProjectId().equals(dto.getGitlabProjectId())) {
            repository.findByGitlabProjectId(dto.getGitlabProjectId()).ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw new IllegalArgumentException("GitLab 项目ID已被其他项目占用");
                }
            });
        }
        apply(dto, p);
        ProjectInfo saved = repository.save(p);
        log.info("更新项目配置: id={} gitlabProjectId={}", saved.getId(), saved.getGitlabProjectId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
        log.info("删除项目配置: id={}", id);
    }

    private void apply(ProjectInfoDto dto, ProjectInfo p) {
        p.setGitlabProjectId(dto.getGitlabProjectId());
        p.setProjectName(dto.getProjectName());
        p.setGitUrl(dto.getGitUrl());
        p.setLocalCodePath(dto.getLocalCodePath());
        p.setAgentCommand(dto.getAgentCommand());
        p.setReceiveEmail(dto.getReceiveEmail());
        p.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
    }
}
