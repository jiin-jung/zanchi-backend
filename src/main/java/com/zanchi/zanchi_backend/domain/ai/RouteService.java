package com.zanchi.zanchi_backend.domain.ai;

import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** 회원별 경로 저장. 모든 조회·삭제는 소유자 기준으로만 동작한다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteService {

    private final RoutePlanRepository routePlanRepository;
    private final MemberRepository memberRepository;

    public List<RoutePlan> myRoutes(Long ownerId) {
        return routePlanRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    public Optional<RoutePlan> myRoute(Long id, Long ownerId) {
        return routePlanRepository.findByIdAndOwnerId(id, ownerId);
    }

    @Transactional
    public RoutePlan create(Long ownerId, RoutePlan.RoutePlanBuilder draft, List<String> tags, List<RouteStep> steps) {
        RoutePlan plan = draft.owner(memberRepository.getReferenceById(ownerId)).build();
        plan.setTags(tags);
        plan.setSteps(steps);
        return routePlanRepository.save(plan);
    }

    /** 내 경로면 삭제하고 true. 태그·단계는 cascade로 함께 삭제된다 */
    @Transactional
    public boolean delete(Long id, Long ownerId) {
        return routePlanRepository.findByIdAndOwnerId(id, ownerId)
                .map(plan -> { routePlanRepository.delete(plan); return true; })
                .orElse(false);
    }
}
