package com.scanplatform.dto;

import lombok.Data;

/**
 * 助手消息的点赞/点踩：{@code 1} 点赞，{@code -1} 点踩，{@code null} 清除。
 */
@Data
public class GitQaChatMessageFeedbackRequest {
    /** 1=点赞，-1=点踩，null=清除记录 */
    private Integer feedback;

    public void validate() {
        if (feedback == null) {
            return;
        }
        if (feedback != 1 && feedback != -1) {
            throw new IllegalArgumentException("feedback 只能为 1、-1 或 null");
        }
    }
}
