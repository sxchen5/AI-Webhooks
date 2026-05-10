package com.scanplatform.dto;

import com.scanplatform.agent.AgentCliKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgentModelOptionDto {
    private Long id;
    @NotNull
    private AgentCliKind cliKind;
    @NotBlank
    private String modelKey;
    private String displayLabel;
    private Integer sortOrder;
    private Integer status;
}
