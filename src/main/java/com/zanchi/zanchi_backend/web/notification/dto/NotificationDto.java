// com.zanchi.zanchi_backend.web.notification.dto.NotificationDto
package com.zanchi.zanchi_backend.web.notification.dto;

import com.zanchi.zanchi_backend.domain.notification.entity.Notification;
import com.zanchi.zanchi_backend.domain.notification.entity.NotificationType;
import com.zanchi.zanchi_backend.domain.member.ActorBrief;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private Long id;
    private NotificationType type;
    private String text;
    private boolean read;
    private LocalDateTime createdAt;

    // 관련 리소스
    private Long clipId;
    private Long commentId;

    // 요구 필드(평면)
    private Long contentId;        // 알림 자체의 고유 id
    private Long actorId;
    private Long receiverId;
    private String actorNickname;  // Member.name
    private String actorAvatarUrl; // Member.avatarUrl

    /** 기본 매핑 (프로필 없음) */
    public static NotificationDto from(Notification n) {
        return from(n, null);
    }

    /** 프로필 주입 매핑 */
    public static NotificationDto from(Notification n, ActorBrief brief) {
        if (n == null) return null;

        String nickname = (brief != null ? brief.getName() : null);
        String avatarUrl = (brief != null ? brief.getAvatarUrl() : null);

        return NotificationDto.builder()
                .id(n.getId())
                .type(n.getType())
                .text(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .clipId(n.getClipId())
                .commentId(n.getCommentId())
                .contentId(n.getId())         // 핵심: contentId = 알림 id
                .actorId(n.getActorId())
                .receiverId(n.getReceiverId())
                .actorNickname(nickname)
                .actorAvatarUrl(avatarUrl)
                .build();
    }
}
