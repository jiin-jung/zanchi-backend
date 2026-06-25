package com.zanchi.zanchi_backend.domain.login;

import com.zanchi.zanchi_backend.config.jwt.JwtTokenProvider;
import com.zanchi.zanchi_backend.domain.member.Member;
import com.zanchi.zanchi_backend.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * 로그인 성공 시
     *  - first_login_at 원자적 세팅 성공 여부로 "처음 로그인" 판정
     *  - login_count++, last_login_at 업데이트
     *  - JWT 토큰 발급
     *
     * 실패 시 null 반환 (컨트롤러에서 401 처리)
     */
    @Transactional
    public LoginServiceResult login(String loginId, String password) {
        Member member = memberRepository.findByLoginId(loginId)
                .filter(m -> passwordEncoder.matches(password, m.getPassword()))
                .orElse(null);

        if (member == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        // 1) 처음 로그인 선점 시도 (first_login_at이 null일 때만 세팅)
        int claimed = memberRepository.claimFirstLoginIfAbsent(member.getId(), now);
        boolean firstLogin = (claimed == 1);

        // 2) 공통 로그인 통계 갱신
        memberRepository.bumpLoginStats(member.getId(), now);

        // 3) JWT 토큰 발급
        String token = jwtTokenProvider.createToken(member.getLoginId());

        return new LoginServiceResult(
                token,
                firstLogin,
                member.isPreferenceSurveyCompleted()
        );
    }

    /** 서비스 응답 DTO */
    public record LoginServiceResult(
            String token,
            boolean firstLogin,
            boolean preferenceSurveyCompleted
    ) {}
}
