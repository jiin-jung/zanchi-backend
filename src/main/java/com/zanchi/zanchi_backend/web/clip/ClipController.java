package com.zanchi.zanchi_backend.web.clip;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.ClipComment;
import com.zanchi.zanchi_backend.domain.clip.dto.*;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipCommentRepository;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipLikeRepository;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipRepository;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipSaveRepository;
import com.zanchi.zanchi_backend.domain.clip.service.ClipService;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import com.zanchi.zanchi_backend.domain.member_follow.dto.MemberSummary;
import com.zanchi.zanchi_backend.domain.ranking.dto.ClipRankView;
import com.zanchi.zanchi_backend.web.clip.dto.ClipRankResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.net.URI;

@RestController
@RequiredArgsConstructor
@Validated
public class ClipController {

    private final ClipService clipService;
    private final ClipCommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final ClipRepository clipRepository;
    private final ClipLikeRepository clipLikeRepository;
    private final ClipSaveRepository clipSaveRepository;

    // 2) 피드 (페이지네이션)
    @GetMapping("/api/clips/feed")
    public ResponseEntity<Slice<ClipFeedRes>> feed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size) {

        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 3), // 최대 3개
                Sort.by("createdAt").descending().and(Sort.by("id").descending())
        );

        Slice<Clip> slice = clipService.feedSlice(pageable);

        // Slice 타입이면 map 사용 가능
        Slice<ClipFeedRes> mapped = slice.map(ClipFeedRes::of);
        return ResponseEntity.ok(mapped);
    }

    // 3) 조회수 증가
    @PostMapping("/api/clips/{clipId}/view")
    public ResponseEntity<?> view(@PathVariable Long clipId) {
        clipService.increaseView(clipId);
        return ResponseEntity.ok().build();
    }

    // 4) 좋아요 토글
    @PostMapping("/api/clips/{clipId}/like")
    public ResponseEntity<?> like(@PathVariable Long clipId,
                                  @AuthenticationPrincipal(expression = "member.id") Long memberId) {
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        }
        boolean liked = clipService.toggleLike(clipId, memberId);
        return ResponseEntity.ok(new LikeToggleRes(liked));
    }

    // 4-1) 좋아요 취소 (멱등)
    @DeleteMapping("/api/clips/{clipId}/like")
    public ResponseEntity<?> unlike(@PathVariable Long clipId,
                                    @AuthenticationPrincipal(expression = "member.id") Long memberId) {
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        }
        clipService.unlike(clipId, memberId);
        return ResponseEntity.noContent().build(); // 204
    }

    // 5) 댓글 목록
    @GetMapping("/api/clips/{clipId}/comments")
    public ResponseEntity<Page<ClipCommentRes>> comments(@PathVariable Long clipId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        // 상위 댓글만 조회 (대댓글 제외)
        Page<ClipComment> p = commentRepository.findByClipIdAndParentIsNullOrderByIdAsc(
                clipId, PageRequest.of(page, size)
        );

        var parents = p.getContent();
        var parentIds = parents.stream().map(ClipComment::getId).toList();

        // 부모 댓글별 대댓글 개수 일괄 조회 (N+1 방지)
        var counts = parentIds.isEmpty()
                ? java.util.Map.<Long, Long>of()
                : commentRepository.countRepliesByParentIds(parentIds)
                .stream().collect(java.util.stream.Collectors.toMap(
                        ReplyCountItem::getParentId,
                        ReplyCountItem::getCnt
                ));

        var list = parents.stream()
                .map(c -> ClipCommentRes.of(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();

        return ResponseEntity.ok(new PageImpl<>(list, p.getPageable(), p.getTotalElements()));
    }

    // 5-1) 댓글 작성 (Principal 표현식 통일)
    @PostMapping("/api/clips/{clipId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long clipId,
                                        @AuthenticationPrincipal(expression = "member.id") Long memberId,
                                        @Valid @RequestBody CommentCreateReq req) {
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        }
        ClipComment saved = clipService.addComment(clipId, memberId, req.content());
        return ResponseEntity.ok(ClipCommentRes.of(saved));
    }

    // 대댓글 목록
    @GetMapping("/api/clips/{clipId}/comments/{commentId}/replies")
    public ResponseEntity<Page<ClipCommentRes>> replies(
            @PathVariable Long clipId,
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var p = clipService.getReplies(commentId, PageRequest.of(page, size));
        var list = p.getContent().stream().map(ClipCommentRes::of).toList();
        return ResponseEntity.ok(new PageImpl<>(list, p.getPageable(), p.getTotalElements()));
    }

    // 대댓글 작성 (Principal 표현식 통일)
    @PostMapping("/api/clips/{clipId}/comments/{commentId}/replies")
    public ResponseEntity<?> addReply(
            @PathVariable Long clipId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal(expression = "member.id") Long memberId,
            @Valid @RequestBody CommentCreateReq req) {
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        }
        var saved = clipService.addReply(clipId, commentId, memberId, req.content());
        return ResponseEntity.ok(ClipCommentRes.of(saved));
    }

    // 6) 캡션만 수정 (JSON PATCH)
    @PatchMapping("/api/clips/{clipId}")
    public ResponseEntity<?> updateCaption(
            @PathVariable Long clipId,
            @AuthenticationPrincipal(expression = "member.id") Long memberId,
            @Valid @RequestBody ClipUpdateReq req) {
        if (memberId == null) return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        Clip updated = clipService.updateCaption(clipId, memberId, req.caption());
        return ResponseEntity.ok(ClipUploadRes.of(updated));
    }

    // 8) 삭제
    @DeleteMapping("/api/clips/{clipId}")
    public ResponseEntity<?> deleteClip(
            @PathVariable Long clipId,
            @AuthenticationPrincipal(expression = "member.id") Long memberId) {
        if (memberId == null) return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        clipService.deleteClip(clipId, memberId);
        return ResponseEntity.noContent().build();
    }

    // 내 페이지용(내 계정 기준)
    @GetMapping("/api/me/clips")
    public ResponseEntity<Page<ClipFeedRes>> myClips(
            @AuthenticationPrincipal(expression = "member.id") Long meId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (meId == null) return ResponseEntity.status(401).build();

        Page<Clip> p = clipService.myClips(meId, PageRequest.of(page, size));
        var ids = p.getContent().stream().map(Clip::getId).toList();

        Set<Long> likedSet = ids.isEmpty() ? Set.of()
                : new HashSet<>(clipLikeRepository.findLikedClipIds(meId, ids));

        Set<Long> savedSet = ids.isEmpty() ? Set.of()
                : new HashSet<>(clipSaveRepository.findSavedClipIds(meId, ids));

        return ResponseEntity.ok(
                p.map(c -> ClipFeedRes.of(
                        c,
                        likedSet.contains(c.getId()),
                        savedSet.contains(c.getId())
                ))
        );
    }

    // 다른 유저 페이지용(프로필 방문)
    @GetMapping("/api/members/{userId}/clips")
    public ResponseEntity<Page<ClipFeedRes>> userClips(
            @PathVariable Long userId,
            @AuthenticationPrincipal(expression = "member.id") Long meId, // ← 현재 로그인 사용자(없을 수 있음)
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<Clip> p = clipService.myClips(userId, PageRequest.of(page, size));
        var ids = p.getContent().stream().map(Clip::getId).toList();

        Set<Long> likedSet = (meId != null && !ids.isEmpty())
                ? new HashSet<>(clipLikeRepository.findLikedClipIds(meId, ids))
                : Set.of();

        Set<Long> savedSet = (meId != null && !ids.isEmpty())
                ? new HashSet<>(clipSaveRepository.findSavedClipIds(meId, ids))
                : Set.of();

        return ResponseEntity.ok(
                p.map(c -> ClipFeedRes.of(
                        c,
                        likedSet.contains(c.getId()),
                        savedSet.contains(c.getId())
                ))
        );
    }

    // 검색
    @GetMapping("/api/clips/search")
    public ResponseEntity<Page<ClipFeedRes>> searchClips(
            @RequestParam String q,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        var p = clipService.searchClips(q, PageRequest.of(page, size)).map(ClipFeedRes::of);
        return ResponseEntity.ok(p);
    }

    // 저장 토글
    @PostMapping("/api/clips/{clipId}/save")
    public ResponseEntity<SaveToggleRes> toggleSave(
            @PathVariable Long clipId,
            @AuthenticationPrincipal(expression="member.id") Long memberId) {
        if (memberId == null) return ResponseEntity.status(401).build();
        boolean saved = clipService.toggleSave(clipId, memberId);
        return ResponseEntity.ok(new SaveToggleRes(saved));
    }

    // 내가 저장한 클립 목록
    @GetMapping("/api/me/saved")
    public ResponseEntity<Page<ClipFeedRes>> mySaved(
            @AuthenticationPrincipal(expression="member.id") Long meId,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="50") int size){
        if (meId == null) return ResponseEntity.status(401).build();
        var p = clipService.savedClips(meId, PageRequest.of(page, size))
                .map(ClipFeedRes::of);
        return ResponseEntity.ok(p);
    }

    // 내가 좋아요한(픽) 클립 목록
    @GetMapping("/api/me/picks")
    public ResponseEntity<Page<ClipFeedRes>> myPicks(
            @AuthenticationPrincipal(expression="member.id") Long meId,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="50") int size){
        if (meId == null) return ResponseEntity.status(401).build();
        var p = clipService.pickClips(meId, PageRequest.of(page, size))
                .map(ClipFeedRes::of);
        return ResponseEntity.ok(p);
    }

    // 저장 해제 (id 기반)
    @DeleteMapping("/api/clips/{clipId}/save")
    public ResponseEntity<?> unsave(@PathVariable Long clipId,
                                    @AuthenticationPrincipal(expression = "member.id") Long memberId) {
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("error","UNAUTHORIZED"));
        }
        clipService.unsave(memberId, clipId);
        return ResponseEntity.noContent().build();
    }

    // 가장 많은 좋아요를 받은 10개의 클립을 반환하는 API
    @GetMapping("/api/top-liked")
    public List<ClipRankResponse> getTopLikedClips(Pageable pageable) {
        // Pageable 객체는 기본적으로 페이지와 크기를 받습니다. 예를 들어, 처음 10개를 가져옵니다.
        Page<ClipRankView> topClips = clipRepository.findTop10ClipsByLikeCount(pageable);

        // 반환할 DTO 객체로 변환
        List<ClipRankResponse> response = topClips.stream()
                .map(clip -> new ClipRankResponse(
                        clip.getClipId(),
                        clip.getUploaderName(),
                        clip.getLikeCount()))
                .collect(Collectors.toList());

        return response;
    }

    @GetMapping("/api/members/{userId}/summary")
    public ResponseEntity<MemberSummary> userSummary(@PathVariable Long userId) {
        var user = memberRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(MemberSummary.of(user)); // avatarUrl 포함되어야 함
    }

    @GetMapping("/api/me/following/clips")
    public Page<ClipSummary> myFollowingClips(Authentication auth,
                                              @RequestParam(defaultValue = "") String q,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        String loginId = auth.getName(); // JwtAuthenticationFilter 가 세팅
        Long userId = memberRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("로그인 사용자 id를 찾을 수 없습니다: " + loginId));

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return clipService.followingClips(userId, pageable, q);
    }

    // A) S3 업로드 완료 후 메타 저장
    @PostMapping(path = "/api/clips", consumes = "application/json")
    public ResponseEntity<?> createFromS3(
            Authentication auth,
            @Valid @RequestBody ClipCreateReq req) {

        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("error", "UNAUTHORIZED", "msg", "로그인이 필요합니다."));
        }

        String loginId = auth.getName(); // ← 여기!
        Clip saved = clipService.createFromS3ByLoginId(loginId, req.getCaption(), req.getFileKey(), req.getShowId());
        return ResponseEntity.ok(ClipUploadRes.of(saved)); // {id, videoUrl, caption}
    }

    // B) 재생(무중단 전환용 302 redirect)
    @GetMapping("/api/clips/{clipId}/stream")
    public ResponseEntity<Void> stream(@PathVariable Long clipId) {
        Clip clip = clipService.findById(clipId);
        // videoUrl에 S3 정적 URL이 들어있음
        return ResponseEntity.status(302).location(URI.create(clip.getVideoUrl())).build();
    }
}
