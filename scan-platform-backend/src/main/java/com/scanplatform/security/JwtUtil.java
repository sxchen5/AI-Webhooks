package com.scanplatform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 生成与解析（简单登录态，无刷新令牌）。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMs;

    public JwtUtil(
            @Value("${scan.jwt.secret}") String secret,
            @Value("${scan.jwt.expire-hours:24}") long expireHours) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("scan.jwt.secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expireMs = expireHours * 3600_000L;
    }

    public String createToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMs);
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public String parseUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}
