package com.zanchi.zanchi_backend.web.auth;

import com.zanchi.zanchi_backend.domain.auth.LogoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/logout")
public class LogoutController {

    private final LogoutService logoutService;

    @PostMapping
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        logoutService.logout(request, response);
        return ResponseEntity.ok().body("로그아웃 완료");
    }

    @GetMapping   // ← 추가 (개발 편의)
    public ResponseEntity<?> logoutGet(HttpServletRequest req, HttpServletResponse res) {
        logoutService.logout(req, res);
        return ResponseEntity.ok("로그아웃 완료");
    }
}

