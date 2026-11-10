package com.zanchi.zanchi_backend.domain.reservation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.zanchi.zanchi_backend.config.exception.ApiException;
import com.zanchi.zanchi_backend.domain.member.PointService;
import com.zanchi.zanchi_backend.domain.show.Show;
import com.zanchi.zanchi_backend.domain.show.ShowRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ShowRepository showRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationAuditRepository auditRepository;
    private final PointService pointService;
    private final Clock kstClock;

    private static final int MAX_QTY_PER_ORDER = 4;

    @Transactional
    public Reservation create(Integer memberId, Integer showId, int quantity, int pointUsed) {
        if (quantity < 1 || quantity > MAX_QTY_PER_ORDER)
            throw new ApiException(BAD_REQUEST, "quantity must be between 1 and 4");
        if (pointUsed < 0)
            throw new ApiException(BAD_REQUEST, "pointUsed must be >= 0");

        Show show = showRepository.findByIdForUpdate(showId)
                .orElseThrow(() -> new NoSuchElementException("show not found"));

        LocalDateTime now = LocalDateTime.now(kstClock);
        if (now.isBefore(show.getSalesStartAt()) || now.isAfter(show.getSalesEndAt()))
            throw new ApiException(BAD_REQUEST, "sales period closed");

        if (show.getRemainingQty() < quantity)
            throw new ApiException(CONFLICT, "not enough remaining quantity");

        int unitPrice = show.getPrice();
        int totalPrice = Math.max(0, unitPrice * quantity - pointUsed);

        // 포인트 차감
        pointService.usePoints(memberId.longValue(), pointUsed);

        // 재고 차감
        show.setRemainingQty(show.getRemainingQty() - quantity);

        // 예매 생성
        Reservation r = new Reservation();
        r.setMemberId(memberId);
        r.setShow(show);
        r.setQuantity(quantity);
        r.setStatus(ReservationStatus.CONFIRMED);
        r.setUnitPrice(unitPrice);
        r.setTotalPrice(totalPrice);
        r.setPointUsed(pointUsed);
        r.setCreatedAt(now);
        reservationRepository.save(r);

        // 감사 로그
        ReservationAudit audit = new ReservationAudit();
        audit.setReservationId(r.getId());
        audit.setMemberId(memberId);
        audit.setAction("CREATE");
        audit.setDetail("{\"quantity\":" + quantity + ",\"totalPrice\":" + totalPrice + ",\"pointUsed\":" + pointUsed + "}");
        audit.setCreatedAt(now);
        auditRepository.save(audit);

        return r;
    }

    @Transactional
    public void cancel(Integer memberId, Integer reservationId) {
        Reservation r = reservationRepository.findByIdAndMemberId(reservationId, memberId)
                .orElseThrow(() -> new NoSuchElementException("reservation not found"));

        if (r.getStatus() == ReservationStatus.CANCELLED)
            throw new ApiException(BAD_REQUEST, "already cancelled");

        Show show = showRepository.findByIdForUpdate(r.getShow().getId())
                .orElseThrow(() -> new NoSuchElementException("show not found"));

        LocalDateTime now = LocalDateTime.now(kstClock);
        if (!now.isBefore(show.getDate()))
            throw new ApiException(BAD_REQUEST, "cancellation not allowed after show start");

        r.setStatus(ReservationStatus.CANCELLED);
        r.setCanceledAt(now);
        show.setRemainingQty(show.getRemainingQty() + r.getQuantity());
        pointService.refundPoints(memberId.longValue(), r.getPointUsed());

        ReservationAudit audit = new ReservationAudit();
        audit.setReservationId(r.getId());
        audit.setMemberId(memberId);
        audit.setAction("CANCEL");
        audit.setDetail("{\"refundedPoint\":" + r.getPointUsed() + "}");
        audit.setCreatedAt(now);
        auditRepository.save(audit);
    }

    public Page<Reservation> findMyReservations(Integer memberId, int page, int size) {
        return reservationRepository.findByMemberIdOrderByCreatedAtDesc(memberId, PageRequest.of(page, size));
    }
}
