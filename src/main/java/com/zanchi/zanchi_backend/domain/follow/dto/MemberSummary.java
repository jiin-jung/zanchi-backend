package com.zanchi.zanchi_backend.domain.follow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zanchi.zanchi_backend.domain.member.Member;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberSummary(Long id, String name, String loginId, String avatarUrl) {
    public static MemberSummary of(Member m){
        String display = (m.getName()!=null && !m.getName().isBlank()) ? m.getName() : m.getLoginId();
        return new MemberSummary(m.getId(), display, m.getLoginId(), m.getAvatarUrl());
    }
}