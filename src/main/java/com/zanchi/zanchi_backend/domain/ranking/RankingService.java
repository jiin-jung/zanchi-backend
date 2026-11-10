package com.zanchi.zanchi_backend.domain.ranking;

import com.zanchi.zanchi_backend.domain.clip.repository.ClipRepository;
import com.zanchi.zanchi_backend.domain.ranking.dto.ClipRankItem;
import com.zanchi.zanchi_backend.domain.ranking.dto.ClipRankView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;        // 전체 랭킹용 (기존 API 호환)
import java.time.LocalDate;     // 집계 '일자'용
import java.time.LocalDateTime; // 공연별 since용

@Service
@RequiredArgsConstructor
public class RankingService {
    private final ClipRepository clipRepository;

    /** 1) 전체 랭킹 (RankingController가 호출, 기존 API 유지) */
    @Transactional(readOnly = true)
    public Page<ClipRankView> getRanking(Instant since, Pageable pageable) {
        return clipRepository.findRanking(since, pageable);
    }

    /** 2) 공연별 TopN — since(시각) 기준 (현재 네 ShowController 버전과 호환) */
    @Transactional(readOnly = true)
    public Page<ClipRankItem> getRankingByShow(Long showId, LocalDateTime since, Pageable pageable) {
        return clipRepository.findRankingByShowId(showId, since, pageable);
    }

    /** 3) 공연별 TopN — 집계 '일자' 기준 (rankDate 하루 구간) */
    @Transactional(readOnly = true)
    public Page<ClipRankItem> getRankingByShowAndDate(Long showId, LocalDate rankDate, Pageable pageable) {
        LocalDateTime start = rankDate.atStartOfDay();
        LocalDateTime end   = start.plusDays(1);
        return clipRepository.findRankingByShowAndDate(showId, start, end, pageable);
    }

    /** 좋아요 수 상위 클립 */
    public Page<ClipRankView> getTopLiked(Pageable pageable) {
        return clipRepository.findTop10ClipsByLikeCount(pageable);
    }
}
