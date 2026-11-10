package com.zanchi.zanchi_backend.domain.reco.service;

import com.zanchi.zanchi_backend.domain.clip.Clip;
import com.zanchi.zanchi_backend.domain.clip.dto.ClipFeedRes;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipLikeRepository;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipRepository;
import com.zanchi.zanchi_backend.domain.clip.repository.ClipSaveRepository;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import com.zanchi.zanchi_backend.domain.reco.PersonalizeItemMap;
import com.zanchi.zanchi_backend.domain.reco.repository.PersonalizeItemMapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.personalizeruntime.PersonalizeRuntimeClient;
import software.amazon.awssdk.services.personalizeruntime.model.GetRecommendationsRequest;
import software.amazon.awssdk.services.personalizeruntime.model.GetRecommendationsResponse;
import software.amazon.awssdk.services.personalizeruntime.model.PersonalizeRuntimeException;
import software.amazon.awssdk.services.personalizeruntime.model.PredictedItem;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecoService {

    private final PersonalizeRuntimeClient personalizeRuntime;   // AWS SDK v2
    private final MemberRepository memberRepository;
    private final ClipRepository clipRepository;
    private final ClipLikeRepository clipLikeRepository;
    private final ClipSaveRepository clipSaveRepository;
    private final PersonalizeItemMapRepository itemMapRepository;

    @Value("${reco.personalize.campaign-arn:}")
    private String campaignArn;

    @Value("${reco.personalize.enabled:true}")
    private boolean personalizeEnabled;

    // 추천 결과가 DB와 불일치할 수 있으니 넉넉히 받아서 건질 확률을 올림
    private static final int WANT_MULTIPLIER = 10;

    public Page<ClipFeedRes> getMyFeed(String loginId, int page, int size) {
        Long userId = memberRepository.findIdByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("로그인 사용자 id를 찾을 수 없습니다: " + loginId));

        final Pageable pageable = PageRequest.of(page, size);

        // 기능 토글: 비활성화면 곧장 폴백
        if (!personalizeEnabled) {
            log.warn("[reco] disabled by config -> trending fallback");
            return trendingFeed(pageable, userId);
        }
        if (!StringUtils.hasText(campaignArn)) {
            log.error("[reco] empty campaignArn -> trending fallback");
            return trendingFeed(pageable, userId);
        }

        try {
            final int want = Math.max(size * WANT_MULTIPLIER, size);

            GetRecommendationsResponse resp = personalizeRuntime.getRecommendations(
                    GetRecommendationsRequest.builder()
                            .campaignArn(campaignArn)
                            .userId(String.valueOf(userId))
                            .numResults(want)
                            .build()
            );

            List<String> itemIds = resp.itemList().stream().map(PredictedItem::itemId).toList();
            log.info("[reco] raw itemIds userId={} count={} sample(<=20)={}",
                    userId, itemIds.size(), itemIds.stream().limit(20).toList());

            if (itemIds.isEmpty()) {
                log.warn("[reco] Personalize returned empty -> trending fallback");
                return trendingFeed(pageable, userId);
            }

            // 1) 숫자/clip_123 파싱 + 2) 매핑 테이블 조회 (원본 순서 보존)
            Map<String, Long> tableMap = Map.of();
            List<String> unmapped = new ArrayList<>();

            List<Long> clipIdsOrdered = new ArrayList<>(itemIds.size());
            for (String iid : itemIds) {
                Optional<Long> parsed = parseClipId(iid);
                if (parsed.isPresent()) {
                    clipIdsOrdered.add(parsed.get());
                } else {
                    unmapped.add(iid);
                }
            }

            if (!unmapped.isEmpty()) {
                var rows = itemMapRepository.findByItemIdIn(unmapped);
                if (!rows.isEmpty()) {
                    tableMap = rows.stream()
                            .collect(Collectors.toMap(PersonalizeItemMap::getItemId, PersonalizeItemMap::getClipId));
                }
            }

            if (!tableMap.isEmpty()) {
                for (String iid : itemIds) {
                    if (clipIdsOrdered.size() >= itemIds.size()) break;
                    Long cid = tableMap.get(iid);
                    if (cid != null) clipIdsOrdered.add(cid);
                }
            }

            // ===== [추가] DB 존재 검증 + 중복 제거 (앞쪽 우선) =====
            if (clipIdsOrdered.isEmpty()) {
                log.warn("[reco] mappedClipIds empty -> trending fallback; rawItemIds={}", itemIds);
                return trendingFeed(pageable, userId);
            }

            List<Long> existingIds = clipRepository.findExistingIds(clipIdsOrdered);
            Set<Long> existingSet = new HashSet<>(existingIds);

            LinkedHashSet<Long> dedupOrdered = new LinkedHashSet<>();
            for (Long id : clipIdsOrdered) {
                if (existingSet.contains(id)) dedupOrdered.add(id);
            }

            List<Long> orderedClipIds = new ArrayList<>(dedupOrdered);
            if (orderedClipIds.isEmpty()) {
                log.warn("[reco] after existence check, no IDs -> trending fallback; rawItemIds={}", itemIds);
                return trendingFeed(pageable, userId);
            }

            // DB 조회 후 추천 순서 유지
            Map<Long, Clip> byId = clipRepository.findAllById(orderedClipIds).stream()
                    .collect(Collectors.toMap(Clip::getId, c -> c));
            List<Clip> ordered = orderedClipIds.stream()
                    .map(byId::get)
                    .filter(Objects::nonNull)
                    .toList();

            if (ordered.isEmpty()) {
                log.warn("[reco] ordered clips empty after DB fetch -> trending fallback; rawItemIds={}", itemIds);
                return trendingFeed(pageable, userId);
            }

            // 페이지 슬라이스
            int from = Math.min(page * size, ordered.size());
            int to = Math.min(from + size, ordered.size());
            List<Clip> pageList = new ArrayList<>(ordered.subList(from, to));

            // ===== [강화] 보강 시, 추천 전체 + 현재 페이지를 exclude로 묶어 중복 방지 =====
            if (pageList.size() < size) {
                int need = size - pageList.size();
                Set<Long> exclude = new HashSet<>(orderedClipIds);
                exclude.addAll(pageList.stream().map(Clip::getId).toList());
                log.info("[reco] fallback fill need={}, exclude(size)={}", need, exclude.size());
                pageList.addAll(trendingFill(new ArrayList<>(exclude), need));
            }

            // ===== [안전망] 그래도 비면 하드 폴백 =====
            if (pageList.isEmpty()) {
                log.error("[reco] pageList empty even after fill -> hard trending fallback");
                return trendingFeed(pageable, userId);
            }

            // 나의 좋아요/저장 표시
            Set<Long> idsForMark = pageList.stream().map(Clip::getId).collect(Collectors.toSet());
            Set<Long> liked = idsForMark.isEmpty() ? Set.of()
                    : new HashSet<>(clipLikeRepository.findLikedClipIds(userId, new ArrayList<>(idsForMark)));
            Set<Long> saved = idsForMark.isEmpty() ? Set.of()
                    : new HashSet<>(clipSaveRepository.findSavedClipIds(userId, new ArrayList<>(idsForMark)));

            List<ClipFeedRes> res = pageList.stream()
                    .map(c -> ClipFeedRes.of(c, liked.contains(c.getId()), saved.contains(c.getId())))
                    .toList();

            long total = Math.max(ordered.size(), (long) (page * size + res.size()));
            return new PageImpl<>(res, pageable, total);

        } catch (PersonalizeRuntimeException e) {
            String msg = (e.awsErrorDetails() != null && e.awsErrorDetails().errorMessage() != null)
                    ? e.awsErrorDetails().errorMessage()
                    : e.getMessage();
            log.error("[reco] GetRecommendations failed: {}", msg, e);
            return trendingFeed(pageable, userId); // 장애 시 폴백
        } catch (Exception e) {
            log.error("[reco] exception: {}", e.getMessage(), e);
            return trendingFeed(pageable, userId); // 예외 시 폴백
        }
    }

    /** 폴백 피드: 최신 업로드(또는 트렌딩) */
    private Page<ClipFeedRes> trendingFeed(Pageable pageable, Long userId) {
        Page<Clip> pageClips = clipRepository.findAll(
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        Set<Long> ids = pageClips.getContent().stream().map(Clip::getId).collect(Collectors.toSet());
        Set<Long> liked = ids.isEmpty() ? Set.of()
                : new HashSet<>(clipLikeRepository.findLikedClipIds(userId, new ArrayList<>(ids)));
        Set<Long> saved = ids.isEmpty() ? Set.of()
                : new HashSet<>(clipSaveRepository.findSavedClipIds(userId, new ArrayList<>(ids)));

        List<ClipFeedRes> res = pageClips.getContent().stream()
                .map(c -> ClipFeedRes.of(c, liked.contains(c.getId()), saved.contains(c.getId())))
                .toList();

        return new PageImpl<>(res, pageable, pageClips.getTotalElements());
    }

    /** 보강용 폴백 아이템: 최신 업로드에서 excludes를 제외하고 need만큼 채움 */
    private List<Clip> trendingFill(List<Long> excludes, int need) {
        if (need <= 0) return List.of();
        int fetch = Math.max(need * 5, need * 3); // 여유 폭 확대
        Page<Clip> recent = clipRepository.findAll(
                PageRequest.of(0, fetch, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        Set<Long> excludeSet = (excludes == null) ? Set.of() : new HashSet<>(excludes);

        List<Clip> out = new ArrayList<>(need);
        for (Clip c : recent.getContent()) {
            if (excludeSet.contains(c.getId())) continue;
            out.add(c);
            if (out.size() >= need) break;
        }
        return out;
    }

    // ========= ITEM_ID → Clip.id 파싱 =========
    private static final Pattern CLIP_PREFIX_NUM = Pattern.compile("^clip[_-]?0*(\\d+)$");

    private Optional<Long> parseClipId(String itemId) {
        if (itemId == null || itemId.isBlank()) return Optional.empty();

        // 순수 숫자
        boolean allDigits = true;
        for (int i = 0; i < itemId.length(); i++) {
            if (!Character.isDigit(itemId.charAt(i))) { allDigits = false; break; }
        }
        if (allDigits) {
            try { return Optional.of(Long.parseLong(itemId)); }
            catch (NumberFormatException ignore) { return Optional.empty(); }
        }

        // clip_XXXX / clip-XXXX (선행 0 허용)
        Matcher m = CLIP_PREFIX_NUM.matcher(itemId);
        if (m.matches()) {
            try { return Optional.of(Long.parseLong(m.group(1))); }
            catch (NumberFormatException ignore) { return Optional.empty(); }
        }

        return Optional.empty();
    }
}
