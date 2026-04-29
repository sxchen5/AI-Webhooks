package com.scanplatform.web;

import com.scanplatform.common.ApiResponse;
import com.scanplatform.entity.ActiveScanLog;
import com.scanplatform.service.ActiveScanLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/active-scan/logs")
@RequiredArgsConstructor
public class ActiveScanLogController {

    private final ActiveScanLogQueryService queryService;

    @GetMapping
    public ApiResponse<Page<ActiveScanLog>> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long repoId,
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) String repoName) {
        return ApiResponse.ok(queryService.page(repoId, jobName, repoName,
                PageRequest.of(page, size, Sort.by("id").descending())));
    }

    @GetMapping("/{id}")
    public ApiResponse<ActiveScanLog> detail(@PathVariable Long id) {
        return ApiResponse.ok(queryService.get(id));
    }
}
