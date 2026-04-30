package com.scanplatform.web;

import com.scanplatform.common.ApiResponse;
import com.scanplatform.dto.PlatformSkillDto;
import com.scanplatform.dto.PlatformSkillOptionDto;
import com.scanplatform.entity.PlatformSkill;
import com.scanplatform.service.PlatformSkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform-skills")
@RequiredArgsConstructor
public class PlatformSkillController {

    private final PlatformSkillService service;

    @GetMapping("/options/enabled")
    public ApiResponse<List<PlatformSkillOptionDto>> enabledOptions() {
        return ApiResponse.ok(service.listEnabledOptions());
    }

    @GetMapping
    public ApiResponse<Page<PlatformSkill>> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(
                service.page(keyword, PageRequest.of(page, size, Sort.by("id").descending())));
    }

    @GetMapping("/{id}")
    public ApiResponse<PlatformSkill> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    public ApiResponse<PlatformSkill> create(@Valid @RequestBody PlatformSkillDto dto) {
        return ApiResponse.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<PlatformSkill> update(@PathVariable Long id, @Valid @RequestBody PlatformSkillDto dto) {
        return ApiResponse.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok();
    }
}
