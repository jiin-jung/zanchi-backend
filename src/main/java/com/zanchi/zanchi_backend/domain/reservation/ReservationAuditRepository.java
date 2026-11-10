package com.zanchi.zanchi_backend.domain.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationAuditRepository extends JpaRepository<ReservationAudit, Long> { }
