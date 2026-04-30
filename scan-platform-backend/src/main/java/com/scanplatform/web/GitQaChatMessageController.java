package com.scanplatform.web;

import com.scanplatform.common.ApiResponse;
import com.scanplatform.entity.GitQaChatMessage;
import com.scanplatform.service.GitQaChatMessageService;
import com.scanplatform.service.GitQaProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-git-qa/projects/{projectId}/messages")
@RequiredArgsConstructor
public class GitQaChatMessageController {

    private final GitQaChatMessageService chatMessageService;
    private final GitQaProjectService projectService;

    @GetMapping
    public ApiResponse<Page<GitQaChatMessage>> page(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        projectService.get(projectId);
        return ApiResponse.ok(
                chatMessageService.page(projectId, PageRequest.of(page, size, Sort.by("id").ascending())));
    }

    @DeleteMapping("/{messageId}")
    public ApiResponse<Void> delete(@PathVariable Long projectId, @PathVariable Long messageId) {
        projectService.get(projectId);
        chatMessageService.delete(projectId, messageId);
        return ApiResponse.ok();
    }

    /** 清空该问答配置下全部聊天记录 */
    @DeleteMapping
    public ApiResponse<Void> deleteAll(@PathVariable Long projectId) {
        projectService.get(projectId);
        chatMessageService.deleteAllByProject(projectId);
        return ApiResponse.ok();
    }
}
