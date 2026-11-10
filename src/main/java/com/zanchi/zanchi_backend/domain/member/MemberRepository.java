package com.zanchi.zanchi_backend.domain.member;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

// 알림용 액터 프로필 프로젝션(이름/아바타) 반환을 위해 DTO 프로젝션 인터페이스 import

public interface MemberRepository extends JpaRepository<Member, Long> {

    // === 로그인 아이디로 조회 ===
    Optional<Member> findByLoginId(String loginId);

    // === 회원가입 중복 체크용 ===
    boolean existsByLoginId(String loginId);

    // === 선호도 조사 1회 보장 등 동시성 제어가 필요할 때 사용 ===
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :memberId")
    Optional<Member> findByIdForUpdate(@Param("memberId") Long memberId);

    // === 이름 또는 로그인ID 검색 (대소문자 무시) ===
    Page<Member> findByNameContainingIgnoreCaseOrLoginIdContainingIgnoreCase(
            String name, String loginId, Pageable pageable);

    // === 로그인ID로 ID만 조회 ===
    @Query("select m.id from Member m where m.loginId = :loginId")
    Optional<Long> findIdByLoginId(@Param("loginId") String loginId);

    // === 이름 존재 여부 (대소문자 무시) ===
    boolean existsByNameIgnoreCase(String name);

    // === 다수의 로그인ID로 사용자 목록 조회 ===
    @Query("select m from Member m where m.loginId in :handles")
    List<Member> findAllByLoginIdIn(@Param("handles") Collection<String> handles);

    // ===== 처음 로그인 선점 (first_login_at이 NULL일 때만 세팅) =====
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Member m set m.firstLoginAt = :now where m.id = :memberId and m.firstLoginAt is null")
    int claimFirstLoginIfAbsent(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    // ===== 로그인 통계 갱신 (login_count++, last_login_at=now) =====
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Member m set m.loginCount = m.loginCount + 1, m.lastLoginAt = :now where m.id = :memberId")
    int bumpLoginStats(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    // ===== 알림 액터 프로필 벌크 조회(프로젝션) =====
    // Member.name -> actorNickname, Member.avatarUrl -> actorAvatarUrl 로 매핑될 수 있도록 필드 선택
    @Query("""
        select m.id as id, m.name as name, m.avatarUrl as avatarUrl
        from Member m
        where m.id in :ids
    """)
    List<ActorBrief> findActorBriefs(@Param("ids") Collection<Long> ids);
}
