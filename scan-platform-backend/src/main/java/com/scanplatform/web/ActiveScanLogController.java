package com.scanplatform.web;

import com.scanplatform.common.ApiResponse;
import com.scanplatform.entity.ActiveScanLog;
import com.scanplatform.repository.ActiveScanLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/active-scan/logs")
@RequiredArgsConstructor
public class ActiveScanLogController {

    private final ActiveScanLogRepository repository;

    @GetMapping
    public ApiResponse<Page<ActiveScanLog>> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(repository.findAllByOrderByIdDesc(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ActiveScanLog> detail(@PathVariable Long id) {
        return ApiResponse.ok(repository.findById(id).orElseThrow(() -> new IllegalArgumentException("日志不存在")));
    }
}
