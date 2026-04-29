package com.scanplatform.service;

import com.scanplatform.dto.GitQaChatRequest;
import com.scanplatform.dto.GitQaProjectDto;
import com.scanplatform.entity.GitProject;
import com.scanplatform.entity.GitQaProject;
import com.scanplatform.repository.GitProjectRepository;
import com.scanplatform.repository.GitQaProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitQaProjectService {

    private final GitQaProjectRepository repository;
    private final GitProjectRepository gitProjectRepository;
    private final GitWorkspaceService gitWorkspaceService;
    private final ShellCommandService shellCommandService;
    private final AgentCommandBuilder agentCommandBuilder;

    @Transactional(readOnly = true)
    public Page<GitQaProject> page(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public GitQaProject get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("配置不存在"));
    }

    @Transactional
    public GitQaProject create(GitQaProjectDto dto) {
        GitQaProject e = new GitQaProject();
        apply(dto, e, true);
        return repository.save(e);
    }

    @Transactional
    public GitQaProject update(Long id, GitQaProjectDto dto) {
        GitQaProject e = get(id);
        apply(dto, e, false);
        return repository.save(e);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
        log.info("删除 Git 项目 AI 问答配置: id={}", id);
    }

    /**
     * 同步：克隆/拉取仓库后执行 agent，返回完整输出（前端打字机展示）。
     */
    public GitQaChatResult chat(Long id, GitQaChatRequest req) throws Exception {
        GitQaProject qp = get(id);
        if (qp.getStatus() == null || qp.getStatus() != 1) {
            throw new IllegalArgumentException("该问答配置已禁用");
        }
        String branch = StringUtils.hasText(qp.getBranch()) ? qp.getBranch() : "main";
        String[] sync = gitWorkspaceService.ensureRepoClone(
                qp.getGitUrl(),
                qp.getGitUsername(),
                qp.getGitPassword(),
                branch,
                qp.getLocalClonePath(),
                qp.getId(),
                "git-qa-");
        String workPath = sync[0];
        String commit = sync[1];
        String cloneLog = sync[2] != null ? sync[2] : "";

        String cmd = agentCommandBuilder.resolveGitQaCommand(qp, workPath, branch, commit, req.getQuestion());
        Map<String, String> env = gitQaEnv(qp, workPath, branch, commit);
        ShellCommandService.ShellResult result = shellCommandService.execute(cmd, Path.of(workPath), env);
        boolean success = result.exitCode() == 0;
        String out = result.output();
        if (!success) {
            StringBuilder err = new StringBuilder();
            if (StringUtils.hasText(cloneLog)) {
                err.append("【Git 同步日志】\n").append(cloneLog).append("\n\n");
            }
            err.append("【命令】\n").append(cmd).append("\n\n【输出】\n").append(out);
            out = err.toString();
        }
        return new GitQaChatResult(cmd, out, result.exitCode(), workPath, commit, success);
    }

    private void apply(GitQaProjectDto dto, GitQaProject e, boolean isNew) {
        e.setBotName(dto.getBotName().trim());
        if (dto.getGitProjectId() != null) {
            GitProject gp = gitProjectRepository.findById(dto.getGitProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Git 项目不存在"));
            if (gp.getStatus() == null || gp.getStatus() != 1) {
                throw new IllegalArgumentException("所选 Git 项目已禁用，请启用后重试或改为手动填写");
            }
        }
        e.setGitProjectId(dto.getGitProjectId());
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
        e.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
    }

    private static Map<String, String> gitQaEnv(GitQaProject qp, String path, String branch, String commit) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("GIT_QA_PROJECT_ID", qp.getId() != null ? String.valueOf(qp.getId()) : "");
        m.put("GIT_QA_BOT_NAME", qp.getBotName() != null ? qp.getBotName() : "");
        m.put("GIT_QA_REPO_PATH", path);
        m.put("GIT_QA_BRANCH", branch);
        m.put("GIT_QA_COMMIT", commit != null ? commit : "");
        m.put("GIT_QA_GIT_URL", qp.getGitUrl() != null ? qp.getGitUrl() : "");
        m.put("ACTIVE_SCAN_REPO_PATH", path);
        m.put("ACTIVE_SCAN_BRANCH", branch);
        m.put("ACTIVE_SCAN_COMMIT", commit != null ? commit : "");
        m.put("WEBHOOK_REPO_PATH", path);
        m.put("WEBHOOK_BRANCH", branch);
        m.put("WEBHOOK_COMMIT", commit != null ? commit : "");
        m.put("WEBHOOK_PROJECT_NAME", qp.getBotName() != null ? qp.getBotName() : "");
        m.put("WEBHOOK_GIT_URL", qp.getGitUrl() != null ? qp.getGitUrl() : "");
        return m;
    }

    public record GitQaChatResult(String execCommand, String output, int exitCode, String workPath, String commitHash,
                                   boolean success) {
    }
}
