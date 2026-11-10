package com.zanchi.zanchi_backend.web.member;

import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import com.zanchi.zanchi_backend.domain.member_follow.dto.MemberSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me")
public class ProfileController {

    private final MemberRepository memberRepository;

    // 내 프로필 요약(아바타 포함)
    @GetMapping("/summary")
    public ResponseEntity<MemberSummary> me(@AuthenticationPrincipal(expression="member.id") Long meId) {
        if (meId == null) return ResponseEntity.status(401).build();
        var me = memberRepository.findById(meId).orElseThrow();
        return ResponseEntity.ok(MemberSummary.of(me));
    }

    // 아바타 제거(기본 이미지로)
    @DeleteMapping("/avatar")
    public ResponseEntity<?> deleteAvatar(@AuthenticationPrincipal(expression="member.id") Long meId) {
        if (meId == null) return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        var me = memberRepository.findById(meId).orElseThrow();
        me.setAvatarUrl(null);
        return ResponseEntity.noContent().build();
    }
}