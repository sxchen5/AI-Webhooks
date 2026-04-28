package com.scanplatform.web;

import com.scanplatform.common.ApiResponse;
import com.scanplatform.dto.SysConfigDto;
import com.scanplatform.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统全局配置读写。
 */
@RestController
@RequestMapping("/api/sys-config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @GetMapping
    public ApiResponse<SysConfigDto> get() {
        return ApiResponse.ok(sysConfigService.get());
    }

    @PutMapping
    public ApiResponse<SysConfigDto> save(@RequestBody SysConfigDto dto) {
        return ApiResponse.ok(sysConfigService.save(dto));
    }
}
