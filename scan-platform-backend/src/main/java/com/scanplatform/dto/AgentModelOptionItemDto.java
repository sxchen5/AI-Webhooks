package com.scanplatform.dto;

import lombok.Data;

@Data
public class AgentModelOptionItemDto {
    private String modelKey;
    private String displayLabel;

    public static AgentModelOptionItemDto of(String modelKey, String displayLabel) {
        AgentModelOptionItemDto d = new AgentModelOptionItemDto();
        d.setModelKey(modelKey);
        d.setDisplayLabel(displayLabel);
        return d;
    }
}
