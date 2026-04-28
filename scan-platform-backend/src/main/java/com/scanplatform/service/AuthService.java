package com.scanplatform.service;

import com.scanplatform.dto.LoginRequest;
import com.scanplatform.dto.LoginResponse;
import com.scanplatform.entity.SysUser;
import com.scanplatform.repository.SysUserRepository;
import com.scanplatform.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录校验：查询 sys_user，BCrypt 校验密码，签发 JWT。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        SysUser user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new IllegalArgumentException("账号已禁用");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = jwtUtil.createToken(user.getUsername());
        log.info("用户登录成功: username={}", user.getUsername());
        return new LoginResponse(token, user.getUsername());
    }
}
