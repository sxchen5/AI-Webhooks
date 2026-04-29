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
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/{id}/chat")
    public ApiResponse<GitQaProjectService.GitQaChatResult> chat(
            @PathVariable Long id,
            @Valid @RequestBody GitQaChatRequest body) throws Exception {
        return ApiResponse.ok(service.chat(id, body));
    }
}
