package com.scanplatform.service;

import com.scanplatform.dto.ActiveScanRepoDto;
import com.scanplatform.entity.ActiveScanRepo;
import com.scanplatform.repository.ActiveScanJobRepository;
import com.scanplatform.repository.ActiveScanRepoRepository;
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
public class ActiveScanRepoService {

    private final ActiveScanRepoRepository repository;
    private final ActiveScanJobRepository jobRepository;

    @Transactional(readOnly = true)
    public Page<ActiveScanRepo> page(String projectName, Pageable pageable) {
        if (!StringUtils.hasText(projectName)) {
            return repository.findAll(pageable);
        }
        String n = projectName.trim();
        return repository.findAll((root, q, cb) -> cb.equal(root.get("repoName"), n), pageable);
    }

    @Transactional(readOnly = true)
    public List<ActiveScanRepo> listForOptions() {
        return repository.findAll(org.springframework.data.domain.Sort.by("repoName"));
    }

    @Transactional(readOnly = true)
    public ActiveScanRepo get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("仓库不存在"));
    }

    @Transactional
    public ActiveScanRepo create(ActiveScanRepoDto dto) {
        ActiveScanRepo e = new ActiveScanRepo();
        apply(dto, e, true);
        return repository.save(e);
    }

    @Transactional
    public ActiveScanRepo update(Long id, ActiveScanRepoDto dto) {
        ActiveScanRepo e = get(id);
        apply(dto, e, false);
        return repository.save(e);
    }

    @Transactional
    public void delete(Long id) {
        if (jobRepository.countByRepoId(id) > 0) {
            throw new IllegalArgumentException("该仓库仍被扫描任务引用，请先删除任务");
        }
        repository.deleteById(id);
        log.info("删除主动扫描仓库: id={}", id);
    }

    private void apply(ActiveScanRepoDto dto, ActiveScanRepo e, boolean isNew) {
        e.setRepoName(dto.getRepoName());
        e.setGitUrl(dto.getGitUrl());
        e.setGitUsername(dto.getGitUsername());
        if (StringUtils.hasText(dto.getGitPassword()) || isNew) {
            e.setGitPassword(StringUtils.hasText(dto.getGitPassword()) ? dto.getGitPassword() : null);
        }
        e.setBranch(StringUtils.hasText(dto.getBranch()) ? dto.getBranch() : "main");
        e.setLocalClonePath(StringUtils.hasText(dto.getLocalClonePath()) ? dto.getLocalClonePath() : null);
        e.setAgentCommand(StringUtils.hasText(dto.getAgentCommand()) ? dto.getAgentCommand() : "(cursor-skill)");
        e.setScanSkillName(StringUtils.hasText(dto.getScanSkillName()) ? dto.getScanSkillName().trim() : null);
        e.setScanSkillPrompt(StringUtils.hasText(dto.getScanSkillPrompt()) ? dto.getScanSkillPrompt().trim() : null);
        e.setReceiveEmail(dto.getReceiveEmail());
        e.setDisplayCommit(dto.getDisplayCommit() != null ? dto.getDisplayCommit() : 1);
        e.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
    }
}
