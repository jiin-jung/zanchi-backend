// com.zanchi.zanchi_backend.web.notification.NotificationController
package com.zanchi.zanchi_backend.web.notification;

import com.zanchi.zanchi_backend.config.security.MemberPrincipal;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import com.zanchi.zanchi_backend.domain.notification.entity.Notification;
import com.zanchi.zanchi_backend.domain.notification.service.NotificationService;
import com.zanchi.zanchi_backend.web.notification.dto.NotificationDto;
import com.zanchi.zanchi_backend.domain.member.ActorBrief;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    @GetMapping
    public ResponseEntity<Page<NotificationDto>> list(
            @AuthenticationPrincipal MemberPrincipal me,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        Long meId = me.getId();
        String rawTag = notificationService.stateTag(meId);

        if (etagMatches(rawTag, ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(rawTag) // ResponseEntity가 필요한 경우 자동으로 quote 처리
                    .cacheControl(CacheControl.noStore())
                    .build();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> entities = notificationService.list(meId, pageable);

        // 액터 프로필 벌크 조회
        Set<Long> actorIds = entities.getContent().stream()
                .map(Notification::getActorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, ActorBrief> briefMap = actorIds.isEmpty()
                ? Collections.emptyMap()
                : memberRepository.findActorBriefs(actorIds).stream()
                .collect(Collectors.toMap(ActorBrief::getId, Function.identity()));

        Page<NotificationDto> body = entities.map(n -> NotificationDto.from(n, briefMap.get(n.getActorId())));

        return ResponseEntity.ok()
                .eTag(rawTag)
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(
            @AuthenticationPrincipal MemberPrincipal me,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        Long meId = me.getId();
        String rawTag = notificationService.stateTag(meId);

        if (etagMatches(rawTag, ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(rawTag)
                    .cacheControl(CacheControl.noStore())
                    .build();
        }

        long count = notificationService.unreadCount(meId);
        return ResponseEntity.ok()
                .eTag(rawTag)
                .cacheControl(CacheControl.noStore())
                .body(count);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal MemberPrincipal me,
            @PathVariable Long id
    ) {
        notificationService.markRead(me.getId(), id);
        String rawTag = notificationService.stateTag(me.getId());
        return ResponseEntity.noContent()
                .eTag(rawTag)
                .cacheControl(CacheControl.noStore())
                .build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal MemberPrincipal me
    ) {
        notificationService.markAllRead(me.getId());
        String rawTag = notificationService.stateTag(me.getId());
        return ResponseEntity.noContent()
                .eTag(rawTag)
                .cacheControl(CacheControl.noStore())
                .build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal MemberPrincipal me,
            @PathVariable Long id
    ) {
        notificationService.delete(me.getId(), id);
        String rawTag = notificationService.stateTag(me.getId());
        return ResponseEntity.noContent()
                .eTag(rawTag)
                .cacheControl(CacheControl.noStore())
                .build();
    }

    // ===== ETag 유틸 =====
    private static boolean etagMatches(String rawTag, String ifNoneMatch) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank()) return false;
        String token = normalizeIfNoneMatch(ifNoneMatch.trim());
        return rawTag.equals(token);
    }

    private static String normalizeIfNoneMatch(String v) {
        if (v.startsWith("W/")) v = v.substring(2); // 약한 태그 제거
        if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }
}
