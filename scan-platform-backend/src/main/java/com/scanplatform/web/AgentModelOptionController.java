package com.scanplatform.web;

import com.scanplatform.agent.AgentCliKind;
import com.scanplatform.common.ApiResponse;
import com.scanplatform.dto.AgentModelOptionDto;
import com.scanplatform.dto.AgentModelOptionItemDto;
import com.scanplatform.entity.AgentModelOption;
import com.scanplatform.service.AgentModelCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent-models")
@RequiredArgsConstructor
public class AgentModelOptionController {

    private final AgentModelCatalogService catalogService;

    @GetMapping("/options")
    public ApiResponse<List<AgentModelOptionItemDto>> options(@RequestParam(defaultValue = "CURSOR") String cli) {
        return ApiResponse.ok(catalogService.listEnabledOptions(AgentCliKind.fromDb(cli)));
    }

    @GetMapping
    public ApiResponse<List<AgentModelOption>> list() {
        return ApiResponse.ok(catalogService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<AgentModelOption> get(@PathVariable Long id) {
        return ApiResponse.ok(catalogService.get(id));
    }

    @PostMapping
    public ApiResponse<AgentModelOption> create(@Valid @RequestBody AgentModelOptionDto dto) {
        return ApiResponse.ok(catalogService.create(dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<AgentModelOption> update(@PathVariable Long id, @Valid @RequestBody AgentModelOptionDto dto) {
        return ApiResponse.ok(catalogService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        catalogService.delete(id);
        return ApiResponse.ok(null);
    }
}
