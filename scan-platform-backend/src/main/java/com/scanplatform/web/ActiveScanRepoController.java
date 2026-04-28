package com.scanplatform.web;

import com.scanplatform.common.ApiResponse;
import com.scanplatform.dto.ActiveScanRepoDto;
import com.scanplatform.entity.ActiveScanRepo;
import com.scanplatform.service.ActiveScanRepoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/active-scan/repos")
@RequiredArgsConstructor
public class ActiveScanRepoController {

    private final ActiveScanRepoService service;

    @GetMapping
    public ApiResponse<Page<ActiveScanRepo>> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.page(PageRequest.of(page, size, Sort.by("id").descending())));
    }

    @GetMapping("/{id}")
    public ApiResponse<ActiveScanRepo> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    public ApiResponse<ActiveScanRepo> create(@Valid @RequestBody ActiveScanRepoDto dto) {
        return ApiResponse.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<ActiveScanRepo> update(@PathVariable Long id, @Valid @RequestBody ActiveScanRepoDto dto) {
        return ApiResponse.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok();
    }
}
