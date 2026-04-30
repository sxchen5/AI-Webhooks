package com.scanplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobRunResult {
    private Long jobId;
    /** 成功触发时返回 */
    private Long logId;
    /** 失败时返回原因 */
    private String error;
}
