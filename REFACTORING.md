# 리팩토링 기록

멋쟁이사자처럼 13기 최종 결과물은 태그 [`likelion-13th-final`](https://github.com/jiin-jung/zanchi-backend/tree/likelion-13th-final) (2025-08-25)에 있습니다.
이 문서는 그 이후 개인적으로 진행한 리팩토링만 기록합니다. 형식: 문제 → 원인 → 바꾼 방식.

## 완료

| 날짜 | 문제 | 바꾼 방식 |
|---|---|---|
| 2026-06-24 | 회원 삭제 API를 누구나 호출 가능 | 본인 또는 관리자만 삭제하도록 인증 검사 추가 |
| 2026-06-24 | 비밀번호 평문 저장 | BCrypt 암호화 적용 |
| 2026-06-24 | main 푸시마다 EC2 자동 배포 | 배포 워크플로우 자동 실행 비활성화 |
| 2026-06-24 | JWT 시크릿이 코드에 하드코딩 | 외부 설정(환경변수)으로 분리 |
| 2026-06-25 | 같은 URL에 프리사인 컨트롤러 2개 매핑 | 중복 컨트롤러 제거 |
| 2026-06-25 | 예외마다 응답 형식이 제각각 | 공통 예외 응답 처리 보강 |
| 2026-06-25 | 운영 설정 기본값이 개발용으로 박혀 있음 | 운영 설정 기본값 정리 |
| 2026-06-25 | 테스트가 실제 DB에 의존 | 테스트용 H2 프로필 적용 |
| 2026-06-25 | 보안 의존성 중복 선언 | build.gradle 정리 |

## 남은 문제 (2026-11 구조 분석)

1. **domain이 web을 import** — `MemberService`(MemberForm), `MemberRepository`(NotificationDto), `PreferenceService`(web dto). 계층 의존 방향이 거꾸로입니다.
2. **컨트롤러가 리포지토리를 직접 사용** — `ClipController`(리포지토리 5개), `MyRouteController`, `ProfileController`, `MemberController`, `NotificationController`, `ShowController`.
3. **파일 저장이 두 갈래** — `S3StorageService`는 미사용, 실제 업로드는 로컬 디스크(`FileStorageService`). `StorageController`가 프리사인을 한 번 더 구현하고 버킷 설정 키도 다릅니다.
4. **도달 불가능한 코드** — `DateRoutePlanner`와 그 전용 의존(Google Places/Routes 클라이언트, `PlaceEnricher` 등), `Spot`, `ClipRankProjection`.
5. **경로 저장 API가 두 벌** — 메모리 저장 `/routes`(재시작 시 유실, 소유자 없음)와 JPA `/api/me/routes`. 프론트 사용 여부 확인 후 JPA로 일원화.
6. **테스트·운영용 API가 운영에 노출** — `TestKakaoController`, `OpsController`에 `@Profile` 없음.
7. **중복·불필요 추상화** — `ReservationDtos` 2벌(domain 쪽 미사용), 구현체 하나뿐인 `MemberLookupPort/Adapter`.
8. **패키지 규칙 불일치** — 도메인마다 하위 패키지·DTO 위치가 다르고, `AiConfig`는 루트, `reco/`는 domain 밖, 컨트롤러·서비스가 `config/s3` 아래에 있습니다. 기능별 패키지(package-by-feature)로 재배치 예정.
