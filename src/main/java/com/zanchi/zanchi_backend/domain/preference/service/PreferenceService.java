package com.zanchi.zanchi_backend.domain.preference.service;

import com.zanchi.zanchi_backend.domain.member.Member;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import com.zanchi.zanchi_backend.domain.preference.MemberPreference;
import com.zanchi.zanchi_backend.domain.preference.MemberPreferenceId;
import com.zanchi.zanchi_backend.domain.preference.PreferenceTag;
import com.zanchi.zanchi_backend.domain.preference.repository.MemberPreferenceRepository;
import com.zanchi.zanchi_backend.domain.preference.repository.PreferenceTagRepository;
import com.zanchi.zanchi_backend.domain.preference.dto.PreferenceSurveyResultResponse;
import com.zanchi.zanchi_backend.domain.preference.dto.PreferenceTagResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PreferenceService {

    private static final int MAX_SELECTABLE = 6;

    private final MemberRepository memberRepository;
    private final PreferenceTagRepository preferenceTagRepository;
    private final MemberPreferenceRepository memberPreferenceRepository;

    @Transactional
    public PreferenceSurveyResultResponse submitSurvey(Long memberId, List<Long> tagIds) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (member.isPreferenceSurveyCompleted()) {
            throw new IllegalStateException("선호도 조사는 1회만 가능합니다.");
        }

        // 비어있거나 과다 선택 방지
        if (tagIds == null || tagIds.isEmpty()) {
            throw new IllegalArgumentException("최소 1개 이상 선택해야 합니다.");
        }
        if (tagIds.size() > MAX_SELECTABLE) {
            throw new IllegalArgumentException("최대 " + MAX_SELECTABLE + "개까지 선택 가능합니다.");
        }

        // 중복 ID 제거
        List<Long> distinctIds = tagIds.stream().distinct().toList();
        if (distinctIds.size() != tagIds.size()) {
            throw new IllegalArgumentException("중복된 태그가 포함되어 있습니다.");
        }

        // 존재하는 태그인지 검증
        List<PreferenceTag> tags = preferenceTagRepository.findAllByIdIn(distinctIds);
        if (tags.size() != distinctIds.size()) {
            throw new IllegalArgumentException("유효하지 않은 태그 ID가 포함되어 있습니다.");
        }

        // 저장
        LocalDateTime now = LocalDateTime.now();
        for (PreferenceTag tag : tags) {
            MemberPreferenceId id = new MemberPreferenceId(member.getId(), tag.getId());
            MemberPreference mp = MemberPreference.builder()
                    .id(id)
                    .member(member)
                    .preferenceTag(tag)
                    .createdAt(now)
                    .build();
            memberPreferenceRepository.save(mp);
        }

        // 1회만 수행하도록 플래그
        member.markPreferenceSurveyCompleted();
        memberRepository.save(member);

        return new PreferenceSurveyResultResponse(
                member.getId(),
                tags.stream()
                        .map(t -> new PreferenceTagResponse(t.getId(), t.getCode(), t.getName()))
                        .collect(Collectors.toList()),
                true
        );
    }

    @Transactional
    public List<PreferenceTagResponse> listAllTags() {
        return preferenceTagRepository.findAll().stream()
                .map(t -> new PreferenceTagResponse(t.getId(), t.getCode(), t.getName()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<PreferenceTagResponse> myTags(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return memberPreferenceRepository.findByMember(member).stream()
                .map(MemberPreference::getPreferenceTag)
                .map(t -> new PreferenceTagResponse(t.getId(), t.getCode(), t.getName()))
                .collect(Collectors.toList());
    }
}
