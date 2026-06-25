package com.zanchi.zanchi_backend.config.security;

import com.zanchi.zanchi_backend.config.jwt.JwtAuthenticationFilter;
import com.zanchi.zanchi_backend.config.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberDetailsService memberDetailsService;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${auth.dev.ignore-redis:false}")
    private boolean ignoreRedisErrorsInDev;

    // 쉼표로 분리: 예 https://zanchi-frontend.vercel.app,http://localhost:3000,https://zanchi-frontend-w3bh.vercel.app
    @Value("${app.cors.allowed-origins:https://zanchi-frontend.vercel.app,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/storage/presign").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clips/*/stream").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/api/clips/*/stream").permitAll()

                        // 정적 리소스 공개
                        .requestMatchers(
                                "/", "/index.html", "/signup.html", "/login.html", "/members.html",
                                "/reservation-test.html", "/tags-test.html", "/show.html"
                        ).permitAll()

                        // 기타 공개 경로
                        .requestMatchers("/ai/**", "/places/**", "/error").permitAll()

                        // 공개 API
                        .requestMatchers(
                                "/api/login", "/api/auth/**", "/api/signup",
                                "/api/shows/**"
                        ).permitAll()

                        // 공개 멤버 조회 API
                        .requestMatchers(HttpMethod.GET, "/api/members", "/api/members/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/members/summaries").permitAll()

                        // 회원 삭제 API
                        .requestMatchers(HttpMethod.DELETE, "/api/members/*").authenticated()

                        // S3 Presign (임시 공개)
                        .requestMatchers(HttpMethod.POST, "/api/s3/presign-put", "/s3/presign-put").permitAll()

                        // ====== [클립/태그 공개 엔드포인트] ======
                        .requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll()                 // 태그 목록/검색
                        .requestMatchers(HttpMethod.GET, "/api/clips/feed").permitAll()              // 피드
                        .requestMatchers(HttpMethod.GET, "/api/clips/*/comments/**").permitAll()     // 댓글/대댓글 조회
                        .requestMatchers(HttpMethod.POST, "/api/clips/*/view").permitAll()           // 조회수 증가

                        // ====== [클립/태그 인증 필요한 엔드포인트] ======
                        .requestMatchers(HttpMethod.GET,  "/api/me/following/clips").authenticated() // 팔로잉 피드
                        .requestMatchers(HttpMethod.POST, "/api/clips").authenticated()              // 업로드
                        .requestMatchers(HttpMethod.POST, "/api/clips/*/like").authenticated()       // 좋아요 토글
                        .requestMatchers(HttpMethod.POST, "/api/clips/*/comments").authenticated()   // 댓글 작성
                        .requestMatchers(HttpMethod.POST, "/api/clips/*/comments/*/replies").authenticated() // 대댓글 작성

                        // 예약 API
                        .requestMatchers(HttpMethod.POST, "/api/reservations").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/reservations/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reservations/*/cancel").authenticated()

                        // 내 경로 API
                        .requestMatchers(HttpMethod.POST, "/api/me/routes").authenticated()

                        // 프로필/설정 관련
                        .requestMatchers(HttpMethod.PATCH, "/api/name").authenticated()
                        .requestMatchers(HttpMethod.POST,  "/api/preferences/survey").authenticated()

                        // 업로드 메타 저장(JSON)과 기타 API는 인증 필요
                        .requestMatchers(HttpMethod.POST, "/api/clips").authenticated()

                        // 개인화 추천 피드
                        .requestMatchers(HttpMethod.GET, "/api/reco/my-feed").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/reco/**").authenticated()

                        // 그 외 모두 인증
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> res.sendError(401))
                        .accessDeniedHandler((req, res, ex) -> res.sendError(403))
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(
                                jwtTokenProvider, memberDetailsService, redisTemplate, ignoreRedisErrorsInDev
                        ),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        CorsConfiguration config = new CorsConfiguration();

        // 와일드카드(*) 포함 시 패턴 매칭 사용
        boolean usePatterns = origins.stream().anyMatch(o -> o.contains("*"));
        if (usePatterns) {
            config.setAllowedOriginPatterns(origins);
        } else {
            config.setAllowedOrigins(origins);
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        // 필요한 헤더가 다양할 수 있어 와일드카드 허용
        config.setAllowedHeaders(List.of("*"));
        // 프론트에서 Authorization 등을 읽어야 할 경우 노출
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);              // 쿠키/인증정보 포함 요청 허용
        config.setMaxAge(Duration.ofHours(1));        // preflight 캐시

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
