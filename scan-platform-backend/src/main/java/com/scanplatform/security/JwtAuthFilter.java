package com.scanplatform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scanplatform.common.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * 从 Authorization: Bearer &lt;token&gt; 解析 JWT 并写入 SecurityContext。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    /** 首次请求解析 JWT 后写入，供 ASYNC 派发阶段恢复（派发请求常无 Authorization 头） */
    static final String JWT_AUTH_REQUEST_ATTR = JwtAuthFilter.class.getName() + ".AUTHENTICATION";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // SSE / StreamingResponseBody 会在 ASYNC 派发阶段再次进入过滤器链；需再次解析 JWT，否则会 AccessDenied
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            Object saved = request.getAttribute(JWT_AUTH_REQUEST_ATTR);
            if (saved instanceof Authentication authentication) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
                return;
            }
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String username = jwtUtil.parseUsername(token);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                request.setAttribute(JWT_AUTH_REQUEST_ATTR, auth);
            } catch (ExpiredJwtException ex) {
                SecurityContextHolder.clearContext();
                writeUnauthorized(response, "登录已过期，请重新登录");
                return;
            } catch (Exception ex) {
                log.debug("JWT 解析失败: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail(401000, message));
    }
}
