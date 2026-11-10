package com.zanchi.zanchi_backend.domain.follow;

import com.zanchi.zanchi_backend.domain.clip.repository.ClipRepository;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import com.zanchi.zanchi_backend.domain.follow.dto.FollowCountsRes;
import com.zanchi.zanchi_backend.domain.follow.dto.FollowRes;
import com.zanchi.zanchi_backend.domain.follow.dto.MemberSummary;
import com.zanchi.zanchi_backend.domain.notification.event.FollowCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowService {

    private final MemberRepository memberRepository;
    private final MemberFollowRepository followRepository;
    private final ClipRepository clipRepository;
    private final ApplicationEventPublisher publisher;  // 이벤트 발행

    public FollowRes follow(Long meId, Long targetId) {
        if (meId == null) throw new AccessDeniedException("UNAUTHORIZED");
        if (meId.equals(targetId)) throw new IllegalArgumentException("SELF_FOLLOW_NOT_ALLOWED");

        boolean created = false;
        if (!followRepository.existsByFollower_IdAndFollowing_Id(meId, targetId)) {
            var me = memberRepository.getReferenceById(meId);
            var target = memberRepository.getReferenceById(targetId);
            followRepository.save(MemberFollow.builder().follower(me).following(target).build());
            created = true;
        }

        // 새로 팔로우가 생성된 경우에만 알림 이벤트 발행
        if (created) {
            // actor = meId(팔로우한 사람), receiver = targetId(팔로우 당한 사람)
            publisher.publishEvent(new FollowCreatedEvent(meId, targetId));
        }

        return counts(meId, targetId, true);
    }

    public FollowRes unfollow(Long meId, Long targetId) {
        if (meId == null) throw new AccessDeniedException("UNAUTHORIZED");
        followRepository.deleteByFollower_IdAndFollowing_Id(meId, targetId);
        return counts(meId, targetId, false);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long meId, Long targetId) {
        if (meId == null) return false;
        return followRepository.existsByFollower_IdAndFollowing_Id(meId, targetId);
    }

    @Transactional(readOnly = true)
    public Page<MemberSummary> followers(Long userId, Pageable pageable) {
        return followRepository.findByFollowing_Id(userId, pageable)
                .map(f -> MemberSummary.of(f.getFollower()));
    }

    @Transactional(readOnly = true)
    public Page<MemberSummary> following(Long userId, Pageable pageable) {
        return followRepository.findByFollower_Id(userId, pageable)
                .map(f -> MemberSummary.of(f.getFollowing()));
    }

    private FollowRes counts(Long meId, Long targetId, boolean followingNow) {
        long followerCnt  = followRepository.countByFollowing_Id(targetId);
        long followingCnt = followRepository.countByFollower_Id(meId);
        return new FollowRes(followingNow, followerCnt, followingCnt);
    }

    @Transactional(readOnly = true)
    public FollowCountsRes counts(Long userId) {
        long posts     = clipRepository.countByUploader_Id(userId);
        long followers = followRepository.countByFollowing_Id(userId);
        long following = followRepository.countByFollower_Id(userId);
        return new FollowCountsRes(posts, followers, following);
    }

    // ---------- 검색용 ----------
    @Transactional(readOnly = true)
    public Page<MemberSummary> followers(Long userId, Pageable pageable, String q) {
        Page<MemberFollow> p = (q == null || q.isBlank())
                ? followRepository.findByFollowing_Id(userId, pageable)
                : followRepository.searchFollowers(userId, q.trim(), pageable);
        return p.map(f -> MemberSummary.of(f.getFollower()));
    }

    @Transactional(readOnly = true)
    public Page<MemberSummary> following(Long userId, Pageable pageable, String q) {
        Page<MemberFollow> p = (q == null || q.isBlank())
                ? followRepository.findByFollower_Id(userId, pageable)
                : followRepository.searchFollowing(userId, q.trim(), pageable);
        return p.map(f -> MemberSummary.of(f.getFollowing()));
    }

    @Transactional(readOnly = true)
    public Page<MemberSummary> searchMembers(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return Page.empty(pageable);
        }
        return memberRepository
                .findByNameContainingIgnoreCaseOrLoginIdContainingIgnoreCase(q, q, pageable)
                .map(MemberSummary::of);
    }
}
