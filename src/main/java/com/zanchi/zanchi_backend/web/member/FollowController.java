package com.zanchi.zanchi_backend.web.member;

import com.zanchi.zanchi_backend.domain.follow.FollowService;
import com.zanchi.zanchi_backend.domain.follow.dto.FollowCountsRes;
import com.zanchi.zanchi_backend.domain.follow.dto.FollowRes;
import com.zanchi.zanchi_backend.domain.follow.dto.MemberSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class FollowController {

    private final FollowService followService;

    // 팔로우
    @PostMapping("/{targetId}/follow")
    public ResponseEntity<FollowRes> follow(@PathVariable Long targetId,
                                            @AuthenticationPrincipal(expression="member.id") Long meId) {
        return ResponseEntity.ok(followService.follow(meId, targetId));
    }

    // 언팔
    @DeleteMapping("/{targetId}/follow")
    public ResponseEntity<FollowRes> unfollow(@PathVariable Long targetId,
                                              @AuthenticationPrincipal(expression="member.id") Long meId) {
        return ResponseEntity.ok(followService.unfollow(meId, targetId));
    }

    // 팔로워 목록(나를 팔로우하는 사람들)
    @GetMapping("/{userId}/followers")
    public Page<MemberSummary> followers(@PathVariable Long userId,
                                         @RequestParam(defaultValue = "") String q,
                                         @RequestParam(defaultValue="0") int page,
                                         @RequestParam(defaultValue="20") int size) {
        return followService.followers(userId, PageRequest.of(page, size), q.trim());
    }

    // 팔로잉 목록(내가 팔로우한 사람들)
    @GetMapping("/{userId}/following")
    public Page<MemberSummary> following(@PathVariable Long userId,
                                         @RequestParam(defaultValue = "") String q,
                                         @RequestParam(defaultValue="0") int page,
                                         @RequestParam(defaultValue="20") int size) {
        return followService.following(userId, PageRequest.of(page, size),q.trim());
    }

    // 관계 상태(내가 이 사람을 팔로우 중인지)
    @GetMapping("/{targetId}/relation")
    public ResponseEntity<?> relation(@PathVariable Long targetId,
                                      @AuthenticationPrincipal(expression="member.id") Long meId) {
        boolean following = followService.isFollowing(meId, targetId);
        return ResponseEntity.ok(java.util.Map.of("following", following));
    }

    // 카운트(프로필 요약에 사용)
    @GetMapping("/{userId}/follow-counts")
    public FollowCountsRes counts(@PathVariable Long userId) {
        return followService.counts(userId);
    }

    //--------------------------------------'
    // 전역 계정 검색 (탭 '계정' 용)
    @GetMapping("/search")
    public Page<MemberSummary> searchMembers(@RequestParam String q,
                                             @RequestParam(defaultValue="0") int page,
                                             @RequestParam(defaultValue="20") int size) {
        return followService.searchMembers(q, PageRequest.of(page, size));
    }
}