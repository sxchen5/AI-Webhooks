package com.scanplatform.web;

import com.scanplatform.common.ApiResponse;
import com.scanplatform.dto.ProjectInfoDto;
import com.scanplatform.entity.ProjectInfo;
import com.scanplatform.service.ProjectInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 项目配置 CRUD。
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectInfoController {

    private final ProjectInfoService projectInfoService;

    @GetMapping
    public ApiResponse<Page<ProjectInfo>> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ProjectInfo> data = projectInfoService.page(PageRequest.of(page, size, Sort.by("id").descending()));
        return ApiResponse.ok(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectInfo> get(@PathVariable Long id) {
        return ApiResponse.ok(projectInfoService.get(id));
    }

    @PostMapping
    public ApiResponse<ProjectInfo> create(@Valid @RequestBody ProjectInfoDto dto) {
        return ApiResponse.ok(projectInfoService.create(dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectInfo> update(@PathVariable Long id, @Valid @RequestBody ProjectInfoDto dto) {
        return ApiResponse.ok(projectInfoService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectInfoService.delete(id);
        return ApiResponse.ok();
    }
}
