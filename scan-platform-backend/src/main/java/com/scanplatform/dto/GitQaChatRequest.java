package com.scanplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GitQaChatRequest {
    @NotBlank
    private String question;

    /** 可选；非空时在 agent 命令末尾追加 {@code --model <值>} */
    private String model;
}
