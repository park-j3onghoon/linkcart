# Phase 2 BE 로드맵: 회원관리

## 목적

Phase 1이 stateless 파싱 서비스였다면 Phase 2는 **사용자 소유 영속 저장소**를 도입한다. OAuth 기반 로그인 + JWT 인증 + 사용자별 상품 리스트 영속화.

## 스코프

### 포함
- OAuth 소셜 로그인 (Google 1개 먼저)
- JWT Access + Refresh Token (rotation)
- 사용자별 상품 저장/조회/삭제 API
- PostgreSQL + Spring Data JPA 도입

### 제외 (후속 Phase 또는 별도 PR)
- Kakao/Apple OAuth (Google 검증 후 추가)
- 이메일+비번 로그인 (비번 저장/복구 복잡도 회피)
- 공유/좋아요 (Phase 3)
- 이메일 인증/비번 찾기 (OAuth만이라 불필요)
- 프론트엔드 OAuth 연동 (FE 트랙)

## 기술 스택

| 영역 | 선택 | 이유 |
|---|---|---|
| DB | PostgreSQL | 실무 표준, 티니어 면접 부합 |
| ORM | Spring Data JPA + Hibernate | 기존 Spring 스택 유지 |
| 마이그레이션 | Flyway | 버전 관리 명확 |
| 인증 | Spring Security + JWT | stateless, 웹/앱 통일 |
| OAuth | Spring Security OAuth2 Client | 공식 표준 |
| 로컬 DB | Docker Compose (Postgres 16) | 재현성 |
| DB 테스트 | Testcontainers (Postgres) | 프로덕션과 동일한 DB로 검증 |

## JWT vs 세션 선택 근거

**JWT 채택** — linkcart는 **웹(Next.js) + 모바일(Expo)** 둘 다 있기 때문.
- 모바일에서 쿠키 관리 불편 → Bearer 헤더가 표준
- 웹/앱 인증 전략 통일 (`Authorization: Bearer ...`)
- stateless (Redis 세션 스토어 인프라 불필요)
- Refresh token rotation + DB 저장으로 revoke 가능 (세션의 장점 흉내)

## 주요 설계 결정

1. **provider + provider_user_id unique**: 같은 사람이 Google 여러 계정이면 각각 다른 User. 이메일 통합은 복잡도 회피.
2. **JWT Refresh token rotation**: reuse 감지 시 전체 token family 무효화 (세션 탈취 방어).
3. **Access token 15분, Refresh 14일**: 업계 일반값.
4. **UserProduct는 ParseResult.Success 결과를 소유권으로 영속**: Phase 1의 파싱 결과를 User 소유 리소스로 저장.
5. **`/api/v1/users/me/**` 네이밍**: 본인 리소스 명시적 표현. 관리자 API (`/users/{id}/**`)는 Phase 3 이후.

## PR 분할 (4개, 각 ~300줄)

| PR | 제목 | 주요 작업 | 의존성 |
|---|---|---|---|
| **P2-1** | DB 인프라 + User 도메인 | Docker compose (Postgres), Spring Data JPA, Flyway V1, User 엔티티/포트/JPA 어댑터 | — |
| **P2-2** | Spring Security + Google OAuth + Access JWT | Spring Security 설정, OAuth2 Client (Google), `/auth/oauth/google`, `/auth/me`, Access JWT 발행 | P2-1 |
| **P2-3** | Refresh Token + 로그아웃 + JWT Filter | RefreshToken 엔티티 + rotation, `/auth/refresh`, `/auth/logout`, JwtAuthenticationFilter | P2-2 |
| **P2-4** | 사용자별 상품 저장 API | UserProduct 엔티티, `POST/GET/DELETE /users/me/products` | P2-3 |

## 도메인 모델 개요

```
User
├─ id: Long (auto-generated)
├─ provider: String ("google" | 후속 "kakao")
├─ providerUserId: String (OAuth sub claim)
├─ email: String
├─ displayName: String?
├─ avatarUrl: String?
├─ createdAt: Instant
└─ updatedAt: Instant
UNIQUE (provider, providerUserId)
INDEX (email)

RefreshToken (P2-3)
├─ id: UUID
├─ userId: Long (FK)
├─ tokenHash: String (SHA-256 저장, plaintext 저장 금지)
├─ issuedAt: Instant
├─ expiresAt: Instant
├─ revokedAt: Instant?
└─ replacedByTokenId: UUID? (rotation 체인)

UserProduct (P2-4)
├─ id: Long
├─ userId: Long (FK)
├─ name, price_amount, price_currency, image_url, source_url, mall, parser_used
├─ createdAt: Instant
UNIQUE (userId, sourceUrl) — 동일 URL 중복 저장 방지
```

## API 엔드포인트 개요

| 메서드 | 경로 | 설명 | PR |
|---|---|---|---|
| POST | `/api/v1/auth/oauth/google` | Google OAuth code → Access/Refresh JWT | P2-2 |
| GET | `/api/v1/auth/me` | 현재 유저 정보 | P2-2 |
| POST | `/api/v1/auth/refresh` | Refresh → 새 Access/Refresh | P2-3 |
| POST | `/api/v1/auth/logout` | Refresh revoke | P2-3 |
| POST | `/api/v1/users/me/products` | 파싱 결과를 유저 소유로 저장 | P2-4 |
| GET | `/api/v1/users/me/products` | 내 상품 목록 | P2-4 |
| DELETE | `/api/v1/users/me/products/{id}` | 내 상품 삭제 | P2-4 |

## 보안 고려

- Refresh token은 **해시(SHA-256)로 저장**. 탈취 시 원본 복구 불가.
- Access token 서명 키는 환경 변수 (`LINKCART_JWT_SECRET`). 길이 32바이트 이상 HMAC.
- OAuth redirect URI는 서버 측 화이트리스트 (정확 매칭).
- `/api/v1/users/me/**`는 JWT 인증 필수. Controller-level SecurityRule.

## 일정 예상

- PR당 평균 3h (구현 + 테스트 + 리뷰 + merge)
- 중간 검토 포함 **2~3 영업일**

Phase 2 착수일: 2026-04-17부터, 완료 목표 ~2026-04-22.
