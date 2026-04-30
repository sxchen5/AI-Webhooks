package com.scanplatform.web;

import com.scanplatform.common.ApiResponse;
import com.scanplatform.dto.GitQaChatRequest;
import com.scanplatform.dto.GitQaProjectDto;
import com.scanplatform.entity.GitQaProject;
import com.scanplatform.service.GitQaProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/ai-git-qa/projects")
@RequiredArgsConstructor
public class GitQaProjectController {

    private final GitQaProjectService service;

    @GetMapping
    public ApiResponse<Page<GitQaProject>> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.page(PageRequest.of(page, size, Sort.by("id").descending())));
    }

    @GetMapping("/{id}")
    public ApiResponse<GitQaProject> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    public ApiResponse<GitQaProject> create(@Valid @RequestBody GitQaProjectDto dto) {
        return ApiResponse.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<GitQaProject> update(@PathVariable Long id, @Valid @RequestBody GitQaProjectDto dto) {
        return ApiResponse.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping(value = "/{id}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> chatStream(
            @PathVariable Long id,
            @Valid @RequestBody GitQaChatRequest body) {
        StreamingResponseBody bodyOut = out -> {
            try {
                service.streamChatSse(id, body, out);
            } catch (Exception e) {
                try {
                    java.io.PrintWriter pw = new java.io.PrintWriter(
                            new java.io.OutputStreamWriter(out, java.nio.charset.StandardCharsets.UTF_8), true);
                    String msg = e.getMessage() != null ? e.getMessage().replace("\\", "\\\\").replace("\"", "\\\"") : "error";
                    pw.print("event: error\n");
                    pw.print("data: {\"message\":\"" + msg + "\"}\n\n");
                    pw.print("event: done\n");
                    pw.print("data: {\"exitCode\":-1,\"success\":false}\n\n");
                    pw.flush();
                } catch (Exception ignored) {
                }
            }
        };
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");
        headers.set(HttpHeaders.CONNECTION, "keep-alive");
        return ResponseEntity.ok().headers(headers).body(bodyOut);
    }
}
