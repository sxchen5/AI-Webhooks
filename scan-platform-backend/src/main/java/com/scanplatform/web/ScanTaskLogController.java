package com.scanplatform.web;

import com.scanplatform.common.ApiResponse;
import com.scanplatform.entity.ScanTaskLog;
import com.scanplatform.service.ScanTaskLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 扫描任务日志分页与详情。
 */
@RestController
@RequestMapping("/api/scan-logs")
@RequiredArgsConstructor
public class ScanTaskLogController {

    private final ScanTaskLogQueryService scanTaskLogQueryService;

    @GetMapping
    public ApiResponse<Page<ScanTaskLog>> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(scanTaskLogQueryService.page(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ScanTaskLog> detail(@PathVariable Long id) {
        return ApiResponse.ok(scanTaskLogQueryService.get(id));
    }
}
