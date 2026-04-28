package com.scanplatform.web;

import com.scanplatform.common.ApiResponse;
import com.scanplatform.dto.ActiveScanJobDto;
import com.scanplatform.entity.ActiveScanJob;
import com.scanplatform.service.ActiveScanJobService;
import com.scanplatform.service.ActiveScanTriggerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/active-scan/jobs")
@RequiredArgsConstructor
public class ActiveScanJobController {

    private final ActiveScanJobService jobService;
    private final ActiveScanTriggerService triggerService;

    @GetMapping
    public ApiResponse<Page<ActiveScanJob>> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(jobService.page(PageRequest.of(page, size, Sort.by("id").descending())));
    }

    @GetMapping("/{id}")
    public ApiResponse<ActiveScanJob> get(@PathVariable Long id) {
        return ApiResponse.ok(jobService.get(id));
    }

    @PostMapping
    public ApiResponse<ActiveScanJob> create(@Valid @RequestBody ActiveScanJobDto dto) {
        return ApiResponse.ok(jobService.create(dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<ActiveScanJob> update(@PathVariable Long id, @Valid @RequestBody ActiveScanJobDto dto) {
        return ApiResponse.ok(jobService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return ApiResponse.ok();
    }

    /** 立即执行一次主动扫描 */
    @PostMapping("/{id}/run")
    public ApiResponse<Map<String, Long>> run(@PathVariable Long id) {
        Long logId = triggerService.triggerManual(id);
        return ApiResponse.ok(Map.of("logId", logId));
    }
}
