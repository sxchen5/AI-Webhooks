package com.scanplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GitQaChatRequest {
    @NotBlank
    private String question;

    /** 可选；非空时在 agent 命令末尾追加 {@code --model <值>} */
    private String model;

    /** 为 true 时表示基于已有用户消息重新生成：不插入新用户行，需同时传 {@link #userMessageId} */
    private Boolean regenerate;

    /** 重新生成时必填：对应库中 USER 消息的 id */
    private Long userMessageId;
}
