package com.scanplatform.web;

import com.scanplatform.common.ApiResponse;
import com.scanplatform.dto.CaptchaResponse;
import com.scanplatform.dto.LoginRequest;
import com.scanplatform.dto.LoginResponse;
import com.scanplatform.service.AuthService;
import com.scanplatform.service.LoginCaptchaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录接口：校验 sys_user，返回 JWT。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginCaptchaService loginCaptchaService;

    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() {
        return ApiResponse.ok(loginCaptchaService.newCaptcha());
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
