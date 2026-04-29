package com.scanplatform.web;

import com.scanplatform.common.ApiResponse;
import com.scanplatform.entity.ActiveScanRepo;
import com.scanplatform.service.ActiveScanRepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/active-scan/repo-options")
@RequiredArgsConstructor
public class ActiveScanRepoOptionsController {

    private final ActiveScanRepoService repoService;

    @GetMapping
    public ApiResponse<List<IdNameOption>> list() {
        return ApiResponse.ok(repoService.listForOptions().stream()
                .map(r -> new IdNameOption(r.getId(), r.getRepoName()))
                .collect(Collectors.toList()));
    }

    public record IdNameOption(Long id, String name) {
    }
}
