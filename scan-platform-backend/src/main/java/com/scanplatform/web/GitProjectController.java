package com.scanplatform.web;

import com.scanplatform.common.ApiResponse;
import com.scanplatform.dto.GitProjectDto;
import com.scanplatform.entity.GitProject;
import com.scanplatform.service.GitProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/active-scan/git-projects")
@RequiredArgsConstructor
public class GitProjectController {

    private final GitProjectService service;

    @GetMapping
    public ApiResponse<Page<GitProject>> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String projectName) {
        return ApiResponse.ok(service.page(projectName, PageRequest.of(page, size, Sort.by("id").descending())));
    }

    @GetMapping("/options")
    public ApiResponse<List<IdNameUrlOption>> options() {
        return ApiResponse.ok(service.listForOptions().stream()
                .map(p -> new IdNameUrlOption(p.getId(), p.getProjectName(), p.getGitUrl()))
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ApiResponse<GitProject> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    public ApiResponse<GitProject> create(@Valid @RequestBody GitProjectDto dto) {
        return ApiResponse.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<GitProject> update(@PathVariable Long id, @Valid @RequestBody GitProjectDto dto) {
        return ApiResponse.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok();
    }

    public record IdNameUrlOption(Long id, String projectName, String gitUrl) {
    }
}
