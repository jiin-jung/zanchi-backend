package com.zanchi.zanchi_backend.domain.reco.service;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.dto.ClipFeedRes;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipRepository;
import com.zanchi.zanchi_backend.domain.ranking.dto.ClipRankView;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.personalizeruntime.PersonalizeRuntimeClient;
import software.amazon.awssdk.services.personalizeruntime.model.GetRecommendationsRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalizeService {

    private final PersonalizeRuntimeClient runtime;
    private final ClipRepository clipRepository;

    @Value("${PERSONALIZE_CAMPAIGN_ARN:}")
    private String campaignArn; // 비어있으면 백업으로 전환

    /** 홈 추천 (디버그용 personalizeUserId 우선) */
    public List<ClipFeedRes> homeRecommendations(Long meId, int size, String personalizeUserId) {
        // 1) Personalize 호출 가능 여부
        if (campaignArn == null || campaignArn.isBlank()) {
            return fallbackTopLiked(size);
        }

        // 2) userId 결정
        String userId = Optional.ofNullable(personalizeUserId)
                .filter(s -> !s.isBlank())
                .orElseGet(() -> meId == null ? "anonymous" : ("U" + meId));

        // 3) 추천 호출
        var req = GetRecommendationsRequest.builder()
                .campaignArn(campaignArn)
                .userId(userId)
                .numResults(size)
                .build();

        var res = runtime.getRecommendations(req);

        // 4) itemId → Clip 매핑
        List<Long> clipIds = new ArrayList<>();
        res.itemList().forEach(i -> {
            String itemId = i.itemId(); // 예: "clip_0055", "clip_seed1"
            // 아래는 "clip_숫자" 형태일 때만 DB id로 간주 (추측한 내용입니다)
            if (itemId.startsWith("clip_")) {
                try {
                    long parsed = Long.parseLong(itemId.substring("clip_".length()));
                    clipIds.add(parsed);
                } catch (NumberFormatException ignore) {}
            }
        });

        // 5) DB 조회 → DTO
        var clips = clipRepository.findAllById(clipIds);
        var dto = clips.stream().map(ClipFeedRes::of).toList();

        // 6) 매핑 결과가 비었으면 인기 상위로 백업
        return dto.isEmpty() ? fallbackTopLiked(size) : dto;
    }

    private List<ClipFeedRes> fallbackTopLiked(int size) {
        // 1) 상위 좋아요 프로젝션 조회 (ClipRankView)
        var page = clipRepository.findTop10ClipsByLikeCount(PageRequest.of(0, size));

        // 2) 프로젝션에서 clipId만 뽑아 순서 리스트로
        List<Long> ids = page.stream()
                .map(ClipRankView::getClipId)
                .toList();

        if (ids.isEmpty()) return List.of();

        // 3) 실제 엔티티를 한 번에 로드 (순서 보장을 위해 map 사용)
        Map<Long, Clip> byId = clipRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Clip::getId, Function.identity()));

        // 4) 원래 순서를 유지하며 DTO로 변환
        List<ClipFeedRes> out = new ArrayList<>();
        for (Long id : ids) {
            Clip c = byId.get(id);
            if (c != null) out.add(ClipFeedRes.of(c));
        }
        return out;
    }
}
