package com.zanchi.zanchi_backend.domain.member;

/** Member 프로필 요약 프로젝션 */
public interface ActorBrief {
    Long getId();
    String getName();       // Member.name
    String getAvatarUrl();  // Member.avatarUrl
}
