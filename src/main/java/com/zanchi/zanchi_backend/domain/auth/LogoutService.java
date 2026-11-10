package com.zanchi.zanchi_backend.domain.auth;

import com.zanchi.zanchi_backend.config.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = jwtTokenProvider.extractAccessTokenFromCookie(request);

        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            String email = jwtTokenProvider.getLoginIdFromToken(accessToken);

            // RefreshToken 삭제
            redisTemplate.delete("RT:" + email);

            // AccessToken 블랙리스트 등록
            long expiration = jwtTokenProvider.getRemainingTime(accessToken);
            redisTemplate.opsForValue().set(accessToken, "logout", expiration, TimeUnit.MILLISECONDS);
        }

        // 쿠키 제거
        Cookie cookie = new Cookie("accessToken", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}


