package com.scanplatform.web;

import com.scanplatform.service.GitlabWebhookService;
import com.scanplatform.service.GitlabWebhookService.WebhookHandleResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * GitLab 统一 WebHook 入口：校验 Token 与 IP，异步触发扫描。
 */
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class GitlabWebhookController {

    private final GitlabWebhookService gitlabWebhookService;

    /**
     * GitLab 默认在请求头 X-Gitlab-Token 中携带 Secret Token。
     */
    @PostMapping("/gitlab")
    public ResponseEntity<String> gitlab(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Gitlab-Token", required = false) String gitlabToken,
            HttpServletRequest request) {
        WebhookHandleResult result = gitlabWebhookService.handle(rawBody, gitlabToken, request);
        if (result.rejected()) {
            log.warn("GitLab WebHook HTTP 403: {}", result.message());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result.message());
        }
        log.debug("GitLab WebHook HTTP 200: {}", result.message());
        return ResponseEntity.ok(result.message());
    }
}
