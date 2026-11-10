package com.zanchi.zanchi_backend.domain.reservation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name = "reservation_audit")
@Getter @Setter
public class ReservationAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Integer reservationId;

    @Column(name = "member_id", nullable = false)
    private Integer memberId;

    private String action; // CREATE, CANCEL

    @Column(columnDefinition = "JSON")
    private String detail;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
