package com.scanplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GitQaChatRequest {
    @NotBlank
    private String question;
}
