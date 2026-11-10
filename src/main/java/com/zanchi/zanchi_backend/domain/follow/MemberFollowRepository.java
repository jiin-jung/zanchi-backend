package com.zanchi.zanchi_backend.domain.follow;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberFollowRepository extends JpaRepository<MemberFollow, Long> {
    boolean existsByFollower_IdAndFollowing_Id(Long followerId, Long followingId);
    void deleteByFollower_IdAndFollowing_Id(Long followerId, Long followingId);

    long countByFollowing_Id(Long userId); // 그 사람의 팔로워 수
    long countByFollower_Id(Long userId);  // 그 사람이 팔로잉 수

    Page<MemberFollow> findByFollowing_Id(Long userId, Pageable pageable); // followers
    Page<MemberFollow> findByFollower_Id(Long userId, Pageable pageable);  // following

    // 검색 적용된 목록 (팔로워)
    @Query("""
           select f
             from MemberFollow f
            where f.following.id = :userId
              and ( lower(f.follower.name)    like lower(concat('%', :q, '%'))
                 or lower(f.follower.loginId) like lower(concat('%', :q, '%')) )
           """)
    Page<MemberFollow> searchFollowers(@Param("userId") Long userId,
                                       @Param("q") String q,
                                       Pageable pageable);

    // 검색 적용된 목록 (팔로잉)
    @Query("""
           select f
             from MemberFollow f
            where f.follower.id = :userId
              and ( lower(f.following.name)    like lower(concat('%', :q, '%'))
                 or lower(f.following.loginId) like lower(concat('%', :q, '%')) )
           """)
    Page<MemberFollow> searchFollowing(@Param("userId") Long userId,
                                       @Param("q") String q,
                                       Pageable pageable);
}