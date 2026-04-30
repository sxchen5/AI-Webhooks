package com.scanplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GitQaChatRequest {
    @NotBlank
    private String question;

    /** 可选；非空时在 agent 命令末尾追加 {@code --model <值>} */
    private String model;

    /** 为 true 时表示基于某条已有用户消息再次生成：会插入一条新的用户消息（内容须一致），并需传 {@link #userMessageId} 用于校验 */
    private Boolean regenerate;

    /** 重新生成时必填：对应库中 USER 消息的 id */
    private Long userMessageId;
}
