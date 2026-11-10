package com.zanchi.zanchi_backend.web.auth;

import com.zanchi.zanchi_backend.domain.auth.LoginService;
import com.zanchi.zanchi_backend.web.auth.form.LoginForm;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;

@Controller
@ResponseBody
@RequiredArgsConstructor
@RequestMapping("/api/login")
public class LoginController {

    private final LoginService loginService;

    @PostMapping
    public ResponseEntity<?> login(@RequestBody LoginForm form, HttpServletResponse response) {
        var result = loginService.login(form.getLoginId(), form.getPassword());

        if (result == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(false, null, null, "로그인 실패: 아이디 또는 비밀번호를 확인하세요."));
        }

        // Authorization 헤더
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + result.token());

        // HttpOnly 쿠키 (HTTPS 배포 기준)
        ResponseCookie cookie = ResponseCookie.from("accessToken", result.token())
                .httpOnly(true)
                .secure(true)          // HTTPS 환경 필수
                .sameSite("None")      // FE/BE 다른 사이트인 경우 필요
                .path("/")
                .maxAge(Duration.ofHours(2))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // JSON 본문 응답
        return ResponseEntity
                .ok()
                .headers(headers)
                .body(new LoginResponse(
                        true,
                        result.firstLogin(),
                        result.preferenceSurveyCompleted(),
                        "로그인 성공: 토큰이 발급되었습니다."
                ));
    }

    public record LoginResponse(
            boolean success,
            Boolean firstLogin,
            Boolean preferenceSurveyCompleted,
            String message
    ) {}
}
