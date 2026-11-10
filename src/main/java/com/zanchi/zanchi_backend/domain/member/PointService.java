package com.zanchi.zanchi_backend.domain.member;

import com.zanchi.zanchi_backend.config.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class PointService {
    private final MemberRepository memberRepository;

    @Transactional
    public void usePoints(Long memberId, int amount) {
        if (amount <= 0) return; // 포인트 사용 안 함
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(BAD_REQUEST, "member not found"));

        // 서비스 레이어에서 명시적으로 검증하고 ApiException 던짐
        if (m.getPoint() < amount) {
            throw new ApiException(BAD_REQUEST, "insufficient points");
        }
        m.usePoint(m.getPoint() - amount);
    }

    @Transactional
    public void refundPoints(Long memberId, int amount) {
        if (amount <= 0) return;
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(BAD_REQUEST, "member not found"));
        m.addPoint(m.getPoint() + amount);
    }
}
