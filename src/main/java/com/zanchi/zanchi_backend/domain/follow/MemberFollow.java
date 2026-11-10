package com.zanchi.zanchi_backend.domain.follow;

import com.zanchi.zanchi_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "member_follows",
        uniqueConstraints = @UniqueConstraint(name="uk_member_follow", columnNames={"follower_id","following_id"}))
public class MemberFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 나(팔로우하는 사람)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id")
    private Member follower;

    // 상대(팔로우 당하는 사람)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "following_id")
    private Member following;

    private LocalDateTime createdAt;

    @PrePersist void onCreate(){ this.createdAt = LocalDateTime.now(); }
}