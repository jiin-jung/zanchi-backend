package com.zanchi.zanchi_backend.config.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final int MIN_SECRET_BYTES = 32;
    private static final String PLACEHOLDER_SECRET = "PASTE_BASE64_SECRET_HERE";

    private final String jwtSecret;
    private Key secretKey;

    public JwtTokenProvider(@Value("${jwt.secret:}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(resolveSecretBytes(jwtSecret));
    }

    public String createToken(String loginId) {
        Date now = new Date();
        // 시간 설정을 통해서 토큰 인증 시간 조절 가능
        long expirationMs = 1000 * 60 * 120;
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(loginId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String getLoginIdFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractAccessTokenFromCookie(HttpServletRequest request) {

        // 운영에서는 Authorization 헤더 제거 권장

        // 1. Authorization 헤더 검사 (테스트용)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. accessToken 쿠키 검사 (실제 운영용)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals("accessToken")) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    public long getRemainingTime(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody()
                .getExpiration()
                .getTime() - System.currentTimeMillis();
    }

    private byte[] resolveSecretBytes(String secret) {
        if (!StringUtils.hasText(secret) || PLACEHOLDER_SECRET.equals(secret.trim())) {
            throw new IllegalStateException("jwt.secret must be provided with at least 256 bits.");
        }

        String trimmed = secret.trim();
        byte[] decoded = tryDecodeBase64(trimmed);
        byte[] keyBytes = decoded != null ? decoded : trimmed.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes for HS256.");
        }

        return keyBytes;
    }

    private byte[] tryDecodeBase64(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
