package com.zanchi.zanchi_backend.web.member;

import com.zanchi.zanchi_backend.domain.member.Member;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import com.zanchi.zanchi_backend.domain.member.MemberService;
import com.zanchi.zanchi_backend.domain.member.dto.ChangeNameRequest;
import com.zanchi.zanchi_backend.domain.member.dto.ChangeNameResponse;
import com.zanchi.zanchi_backend.web.member.form.MemberForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MemberController {

    private final MemberRepository memberRepository;
    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid MemberForm form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(Map.of("status", "fail", "message", errors));
        }

        try {
            memberService.signup(form);
            return ResponseEntity.ok(Map.of("status", "success", "message", "회원가입 성공"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "fail", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "fail", "message", "서버 오류 발생"));
        }
    }

    @GetMapping("/members")
    public ResponseEntity<?> findAllMembers() {
        List<Member> members = memberService.findAll();
        return ResponseEntity.ok(Map.of("status", "success", "members", members));
    }

    @DeleteMapping("/members/{id}")
    public ResponseEntity<?> deleteMember(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "member.id") Long requesterId
    ) {
        if (requesterId == null) {
            return ResponseEntity.status(401).body(Map.of("status", "fail", "message", "로그인이 필요합니다."));
        }

        try {
            memberService.deleteById(id, requesterId);
            return ResponseEntity.ok(Map.of("status", "success", "message", "회원 삭제 완료"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "fail", "message", e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("status", "fail", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "fail", "message", "서버 오류 발생"));
        }
    }

    @PatchMapping("/name")
    public ResponseEntity<?> changeMyName(
            @Valid @RequestBody ChangeNameRequest req,
            @AuthenticationPrincipal(expression = "member.id") Long meId
    ) {
        if (meId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "UNAUTHORIZED"));
        }
        ChangeNameResponse res = memberService.changeName(meId, req.name());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/members/{id}/summary")
    public ResponseEntity<PublicMemberSummary> getPublicSummary(@PathVariable Long id) {
        return memberRepository.findById(id)
                .map(m -> ResponseEntity.ok(PublicMemberSummary.of(m)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/members/summaries")
    public List<PublicMemberSummary> getSummaries(@RequestBody IdsRequest req) {
        var ids = (req.ids == null ? List.<Long>of() : req.ids.stream().distinct().toList());

        var map = memberRepository.findAllById(ids).stream()
                .map(PublicMemberSummary::of)
                .collect(java.util.stream.Collectors.toMap(PublicMemberSummary::id, s -> s));

        return ids.stream().map(map::get).filter(java.util.Objects::nonNull).toList();
    }

    private static record PublicMemberSummary(Long id, String name, String avatarUrl) {
        static PublicMemberSummary of(Member m) {
            String display = (m.getName() != null && !m.getName().isBlank())
                    ? m.getName()
                    : m.getLoginId();
            String avatar = (m.getAvatarUrl() != null && !m.getAvatarUrl().isBlank())
                    ? m.getAvatarUrl()
                    : null;
            return new PublicMemberSummary(m.getId(), display, avatar);
        }
    }

    public static record IdsRequest(List<Long> ids) {}
}
