package com.scanplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 登录图形验证码：前端用 imageBase64 展示 PNG，提交时带上 captchaId。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {
    private String captchaId;
    /** 不含 data:image 前缀的 Base64(PNG) */
    private String imageBase64;
}
