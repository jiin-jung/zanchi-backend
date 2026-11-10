package com.zanchi.zanchi_backend.domain.ai;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoutePlanRepository extends JpaRepository<RoutePlan, Long> {

    // 내 목록
    @EntityGraph(attributePaths = {"steps"})
    List<RoutePlan> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    // 내 한 건 상세
    @EntityGraph(attributePaths = {"steps"})
    Optional<RoutePlan> findByIdAndOwnerId(Long id, Long ownerId);
}