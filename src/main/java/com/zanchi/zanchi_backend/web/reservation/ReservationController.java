package com.zanchi.zanchi_backend.web.reservation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.zanchi.zanchi_backend.config.exception.ApiException;
import com.zanchi.zanchi_backend.config.security.MemberPrincipal;
import com.zanchi.zanchi_backend.domain.reservation.*;
import com.zanchi.zanchi_backend.web.reservation.dto.ReservationDtos;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // 현재 로그인 회원 id (ReservationService는 Integer id를 사용)
    private static Integer currentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof MemberPrincipal me))
            throw new ApiException(UNAUTHORIZED, "Unauthenticated");
        return me.getId().intValue();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationDtos.Response create(@RequestBody ReservationDtos.CreateRequest req) {
        Integer memberId = currentMemberId();                             // 여기로 통일
        Reservation r = reservationService.create(
                memberId, req.getShowId(), req.getQuantity(), req.getPointUsed());
        return ReservationDtos.Response.of(r);
    }

    @GetMapping("/me")
    public Map<String, Object> myReservations(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(required = false) ReservationStatus status) {
        Integer memberId = currentMemberId();                             // ★
        Page<Reservation> p = reservationService.findMyReservations(memberId, page, size);
        List<ReservationDtos.Response> content = p.getContent().stream()
                .filter(r -> status == null || r.getStatus() == status)
                .map(ReservationDtos.Response::of)
                .toList();
        return Map.of("content", content,
                "page", p.getNumber(), "size", p.getSize(), "totalElements", p.getTotalElements());
    }

    @PostMapping("/{reservationId}/cancel")
    public ReservationDtos.CancelResponse cancel(@PathVariable Integer reservationId) {
        Integer memberId = currentMemberId();
        reservationService.cancel(memberId, reservationId);
        return ReservationDtos.CancelResponse.builder()
                .reservationId(reservationId)
                .status(ReservationStatus.CANCELLED)
                .build();
    }
}
