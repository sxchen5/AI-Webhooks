package com.scanplatform.service;

import com.scanplatform.dto.GitQaChatRequest;
import com.scanplatform.dto.GitQaProjectDto;
import com.scanplatform.entity.GitProject;
import com.scanplatform.entity.GitQaChatMessage;
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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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
    private final GitQaChatMessageService gitQaChatMessageService;

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
        gitQaChatMessageService.deleteAllByProject(id);
        repository.deleteById(id);
        log.info("删除 Git 项目 AI 问答配置: id={}", id);
    }

    /**
     * 流式：agent stream-json 按行解析后通过 SSE 推送增量文本，最后 {@code event: done}。
     */
    public void streamChatSse(Long id, GitQaChatRequest req, OutputStream rawOut) throws Exception {
        GitQaProject qp = get(id);
        if (qp.getStatus() == null || qp.getStatus() != 1) {
            throw new IllegalArgumentException("该问答配置已禁用");
        }
        boolean regenerate = Boolean.TRUE.equals(req.getRegenerate());
        Long userMessageId;
        if (regenerate) {
            if (req.getUserMessageId() == null) {
                throw new IllegalArgumentException("重新生成需指定 userMessageId");
            }
            GitQaChatMessage userRow = gitQaChatMessageService.requireUserMessage(id, req.getUserMessageId());
            if (!req.getQuestion().trim().equals(userRow.getContent() != null ? userRow.getContent().trim() : "")) {
                throw new IllegalArgumentException("重新生成时 question 须与该用户消息内容一致");
            }
            userMessageId = userRow.getId();
            gitQaChatMessageService.deleteAssistantAfterUserIfPresent(id, userMessageId);
        } else {
            userMessageId = gitQaChatMessageService.saveUser(id, req.getQuestion());
        }

        // 仅用 \n 写 SSE，避免 Windows 上 PrintWriter.println 产生 CRLF 导致前端按 \n\n 分块失败
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(rawOut, StandardCharsets.UTF_8), true);
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

        String cmd;
        try {
            cmd = resolveChatShellCommand(qp, workPath, branch, commit, req.getQuestion(), req.getModel());
        } catch (Exception e) {
            if (!regenerate) {
                gitQaChatMessageService.delete(id, userMessageId);
            }
            sseJson(pw, "error", "{\"message\":\"" + jsonEscape(e.getMessage()) + "\"}");
            sseDone(pw, -1, false);
            return;
        }
        sseJson(pw, "meta", "{\"userMessageId\":" + userMessageId + ",\"execCommand\":\"" + jsonEscape(cmd) + "\",\"workPath\":\""
                + jsonEscape(workPath) + "\",\"commitHash\":\"" + jsonEscape(commit != null ? commit : "")
                + "\",\"cloneLog\":\"" + jsonEscape(StringUtils.hasText(cloneLog) ? cloneLog : "") + "\"}");

        Map<String, String> env = gitQaEnv(qp, workPath, branch, commit);
        AgentStreamJsonSseExtractor extractor = new AgentStreamJsonSseExtractor();
        StringBuilder rawCapture = new StringBuilder();
        StringBuilder assistantReply = new StringBuilder();
        log.info(
                "Git 问答开始执行 agent: projectId={} botName={} workPath={} branch={} cmd={}",
                qp.getId(),
                qp.getBotName(),
                workPath,
                branch,
                tail(cmd, 2000));
        int exit;
        try {
            exit = shellCommandService.executeStreamingJsonRoots(cmd, Path.of(workPath), env, root -> {
                rawCapture.append(root.toString()).append('\n');
                String delta = extractor.consumeRoot(root);
                if (StringUtils.hasText(delta)) {
                    assistantReply.append(delta);
                    log.info("Git 问答 stream-json 增量 projectId={} +{} 字符", qp.getId(), delta.length());
                    sseJson(pw, "assistant", "{\"delta\":\"" + jsonEscape(delta) + "\"}");
                }
            });
        } catch (Exception e) {
            log.warn("Git 问答 agent 执行异常: projectId={} msg={}", qp.getId(), e.getMessage(), e);
            if (!regenerate) {
                gitQaChatMessageService.delete(id, userMessageId);
            }
            sseJson(pw, "error", "{\"message\":\"" + jsonEscape(e.getMessage()) + "\"}");
            sseDone(pw, -1, false);
            return;
        }
        boolean success = exit == 0;
        String replyText = assistantReply.toString();
        if (StringUtils.hasText(replyText)) {
            Long assistantId = gitQaChatMessageService.saveAssistant(id, replyText);
            sseJson(pw, "saved", "{\"assistantMessageId\":" + (assistantId == null ? "null" : String.valueOf(assistantId)) + "}");
            log.info(
                    "Git 问答 AI 回复完成 projectId={} exitCode={} 总字符={}",
                    qp.getId(),
                    exit,
                    replyText.length());
        } else {
            if (!success && !regenerate) {
                gitQaChatMessageService.delete(id, userMessageId);
            }
            log.info(
                    "Git 问答结束 projectId={} exitCode={} success={} 未解析到助手增量文本（可能 agent 输出格式非 stream-json 或进程无输出） rawTail=\n{}",
                    qp.getId(),
                    exit,
                    success,
                    tail(rawCapture.toString(), 4000));
        }
        if (!success) {
            sseJson(pw, "error", "{\"message\":\"" + jsonEscape("agent 退出码 " + exit)
                    + "\",\"rawTail\":\"" + jsonEscape(tail(rawCapture.toString(), 8000)) + "\"}");
        }
        sseDone(pw, exit, success);
    }

    private static void sseJson(PrintWriter pw, String event, String jsonLine) {
        pw.print("event: ");
        pw.print(event);
        pw.print('\n');
        pw.print("data: ");
        pw.print(jsonLine);
        pw.print('\n');
        pw.print('\n');
        pw.flush();
    }

    private static void sseDone(PrintWriter pw, int exitCode, boolean success) {
        sseJson(pw, "done", "{\"exitCode\":" + exitCode + ",\"success\":" + success + "}");
    }

    private static String jsonEscape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private static String tail(String s, int max) {
        if (s == null || s.length() <= max) {
            return s != null ? s : "";
        }
        return s.substring(s.length() - max);
    }

    private String resolveChatShellCommand(GitQaProject qp, String workPath, String branch, String commit,
                                            String question, String modelRaw)
            throws Exception {
        String base;
        if (StringUtils.hasText(qp.getScanSkillName())) {
            base = agentCommandBuilder.resolveGitQaCommand(qp, workPath, branch, commit, question);
        } else {
            String ac = qp.getAgentCommand();
            if (StringUtils.hasText(ac) && !"(cursor-skill)".equals(ac.trim())) {
                base = agentCommandBuilder.resolveGitQaCommand(qp, workPath, branch, commit, question);
            } else {
                base = agentCommandBuilder.buildGitQaStreamJsonCommand(question);
            }
        }
        return GitQaModelSupport.appendModelFlag(base, modelRaw);
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
        e.setAgentCommand(StringUtils.hasText(dto.getAgentCommand()) ? dto.getAgentCommand().trim() : "");
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
}
