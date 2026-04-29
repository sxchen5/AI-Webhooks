package com.scanplatform.web;

import com.scanplatform.common.ApiResponse;
import com.scanplatform.entity.ProjectInfo;
import com.scanplatform.service.ProjectInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/project-options")
@RequiredArgsConstructor
public class ProjectOptionsController {

    private final ProjectInfoService projectInfoService;

    /** WebHook 项目下拉：id + 项目名称 */
    @GetMapping("/webhook-projects")
    public ApiResponse<List<IdNameOption>> webhookProjects() {
        List<ProjectInfo> all = projectInfoService.listAllForOptions();
        return ApiResponse.ok(all.stream()
                .map(p -> new IdNameOption(p.getId(), p.getProjectName()))
                .collect(Collectors.toList()));
    }

    public record IdNameOption(Long id, String name) {
    }
}
