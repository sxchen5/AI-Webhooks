package com.scanplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanplatform.entity.ProjectInfo;
import com.scanplatform.entity.ScanTaskLog;
import com.scanplatform.entity.SysConfig;
import com.scanplatform.repository.ScanTaskLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 接收 GitLab WebHook：校验 Token 与 IP、解析负载、匹配项目、落库并触发异步扫描。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitlabWebhookService {

    private final ObjectMapper objectMapper;
    private final SysConfigService sysConfigService;
    private final ProjectInfoService projectInfoService;
    private final ScanTaskLogRepository scanTaskLogRepository;
    private final ScanAsyncExecutor scanAsyncExecutor;

    @Transactional
    public WebhookHandleResult handle(String rawBody, String gitlabToken, HttpServletRequest request) {
        SysConfig config = sysConfigService.getEntity();
        if (!validateToken(config, gitlabToken)) {
            return WebhookHandleResult.rejected("Webhook Token 无效");
        }
        if (!validateClientIp(config, request)) {
            return WebhookHandleResult.rejected("客户端 IP 不在白名单");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            return WebhookHandleResult.rejected("JSON 解析失败");
        }
        String objectKind = text(root, "object_kind");
        if (!"push".equals(objectKind)) {
            return WebhookHandleResult.ignored("非 push 事件，已忽略: " + objectKind);
        }
        Long gitlabProjectId = longOrNull(root.path("project").path("id"));
        if (gitlabProjectId == null) {
            return WebhookHandleResult.ignored("缺少 project.id");
        }
        String ref = text(root, "ref");
        String branch = normalizeRef(ref);
        String commit = text(root, "after");
        if ("0000000000000000000000000000000000000000".equals(commit)) {
            return WebhookHandleResult.ignored("删除分支事件，无 after commit");
        }
        String commitUser = firstNonBlank(
                text(root, "user_username"),
                text(root, "user_name"),
                text(root.path("user"), "username"),
                text(root.path("user"), "name")
        );
        String projectName = text(root.path("project"), "name");
        if (!StringUtils.hasText(projectName)) {
            projectName = "project-" + gitlabProjectId;
        }

        ProjectInfo project = projectInfoService.findByGitlabProjectId(gitlabProjectId);
        if (project == null) {
            return WebhookHandleResult.ignored("未配置该 GitLab 项目: " + gitlabProjectId);
        }
        if (project.getStatus() == null || project.getStatus() != 1) {
            return WebhookHandleResult.ignored("项目已禁用: " + gitlabProjectId);
        }

        ScanTaskLog log = new ScanTaskLog();
        log.setProjectId(project.getId());
        log.setGitlabProjectId(gitlabProjectId);
        log.setProjectName(project.getProjectName());
        log.setBranch(branch);
        log.setCommitHash(commit);
        log.setCommitUser(commitUser);
        log.setEmailStatus(0);
        log.setTaskStartTime(LocalDateTime.now());
        log = scanTaskLogRepository.save(log);

        scanAsyncExecutor.executeAsync(log.getId());
        return WebhookHandleResult.accepted("已接收任务，logId=" + log.getId());
    }

    private boolean validateToken(SysConfig config, String headerToken) {
        String expected = config.getWebhookToken();
        if (!StringUtils.hasText(expected)) {
            return true;
        }
        return expected.equals(headerToken);
    }

    private boolean validateClientIp(SysConfig config, HttpServletRequest request) {
        String allow = config.getGitlabAllowIps();
        if (!StringUtils.hasText(allow)) {
            return true;
        }
        String clientIp = resolveClientIp(request);
        for (String ip : allow.split(",")) {
            if (clientIp.equals(ip.trim())) {
                return true;
            }
        }
        log.warn("Webhook IP 被拒绝: {}", clientIp);
        return false;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String normalizeRef(String ref) {
        if (!StringUtils.hasText(ref)) {
            return "";
        }
        if (ref.startsWith("refs/heads/")) {
            return ref.substring("refs/heads/".length());
        }
        if (ref.startsWith("refs/tags/")) {
            return ref.substring("refs/tags/".length());
        }
        return ref;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? "" : v.asText("");
    }

    private static String text(JsonNode parent, String child, String field) {
        return text(parent.path(child), field);
    }

    private static Long longOrNull(JsonNode node) {
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        return node.asLong();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v;
            }
        }
        return "";
    }

    public record WebhookHandleResult(boolean accepted, boolean rejected, String message) {
        public static WebhookHandleResult accepted(String msg) {
            return new WebhookHandleResult(true, false, msg);
        }
        public static WebhookHandleResult ignored(String msg) {
            return new WebhookHandleResult(true, false, msg);
        }
        public static WebhookHandleResult rejected(String msg) {
            return new WebhookHandleResult(false, true, msg);
        }
    }
}
