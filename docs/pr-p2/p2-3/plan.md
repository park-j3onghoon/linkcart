# PR-P2-3: Refresh Token + 로그아웃

## 목적

Access token 15분 만료 후 재로그인 필요한 문제를 Refresh Token rotation으로 해결. 로그아웃 엔드포인트 추가.

## 스코프

### 포함
- `RefreshToken` 엔티티 + Flyway V2 마이그레이션
- `RefreshTokenRepository` 포트/어댑터
- `SecureRandomRefreshTokenGenerator` (opaque token, SHA-256 해시 저장)
- `RefreshTokensUsecase` (rotation + reuse 감지 → family revoke)
- `LogoutUsecase` (refresh token revoke)
- `LoginWithGoogleUsecase` 수정: Access + Refresh 동시 발행
- `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`

### NOT in scope
- UserProduct → P2-4
- 다중 기기 세션 관리 UI (admin) → 후속
- 관리자의 타 사용자 강제 로그아웃 → 후속

## 설계

### Refresh Token 형태
- **Opaque token** (JWT 아님): `SecureRandom` 32 bytes → URL-safe Base64
- 클라이언트에 원본 전달, DB에는 **SHA-256 해시만** 저장 (탈취 시 원본 복구 불가)
- 수명: 14일

### Rotation 흐름 (POST /auth/refresh)
1. Request: `{ "refresh_token": "<opaque>" }`
2. 해시 → DB 조회
3. 검증:
   - 존재하지 않음 → 401
   - 만료 (`expiresAt < now`) → 401
   - 이미 revoked (`revokedAt != null`) → **reuse 감지 → 동일 user의 모든 active refresh revoke** → 401
4. 기존 token revoke (`revokedAt = now`)
5. 새 refresh + access 발행, `replacedByTokenId` 체인 기록
6. Response: `{ access_token, refresh_token, token_type, expires_in }`

### 로그아웃 (POST /auth/logout)
- Request: `{ "refresh_token": "<opaque>" }`
- 해당 refresh token만 revoke (다른 기기 세션 유지)
- Response: 204 No Content

### 스키마 (V2__create_refresh_tokens.sql)

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,  -- SHA-256 hex
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_token_id UUID REFERENCES refresh_tokens(id) ON DELETE SET NULL
);

CREATE INDEX idx_refresh_tokens_user_active
    ON refresh_tokens(user_id) WHERE revoked_at IS NULL;
```

- `gen_random_uuid()` 기본값 (Postgres 13+ 내장).
- `user_id CASCADE`: 사용자 삭제 시 refresh token 함께 삭제.
- Partial index: active token만 빠른 조회.

### 파일 변경

**신규 (+11)**:
- `db/migration/V2__create_refresh_tokens.sql`
- `domain/entity/RefreshToken.kt`
- `domain/port/RefreshTokenRepository.kt`
- `application/auth/port/RefreshTokenGenerator.kt` (port)
- `application/auth/usecase/RefreshTokensUsecase.kt`
- `application/auth/usecase/LogoutUsecase.kt`
- `infrastructure/adapter/auth/SecureRandomRefreshTokenGenerator.kt`
- `infrastructure/adapter/persistence/refreshtoken/RefreshTokenEntity.kt`
- `infrastructure/adapter/persistence/refreshtoken/RefreshTokenJpaRepository.kt`
- `infrastructure/adapter/persistence/refreshtoken/RefreshTokenRepositoryAdapter.kt`
- `infrastructure/adapter/persistence/refreshtoken/RefreshTokenMappers.kt`
- `presentation/dto/RefreshRequest.kt`, `LogoutRequest.kt`
- 테스트들

**수정**:
- `application/auth/usecase/LoginWithGoogleUsecase.kt` (Refresh 발행 포함)
- `presentation/api/AuthController.kt` (`/refresh`, `/logout` 엔드포인트)
- `presentation/dto/OAuthLoginResponse.kt` (refresh_token 필드 추가)
- `application.yml` (`refresh-token-ttl-days`)

### 보안 고려

- Refresh token 원본 절대 DB/로그에 저장 금지. 해시만.
- Reuse 감지 시 family revoke → 세션 탈취 방어.
- Access token과 Refresh token의 분리: Access는 stateless JWT (서명 검증), Refresh는 DB 기반 state 추적.
- Opaque token: 예측 불가 SecureRandom 32 bytes, base64url 인코딩 → 길이 43자.

## 테스트

- `RefreshTokensUsecaseTest`: happy path / 만료 / reuse 감지 → family revoke / 원본 아닌 해시 검증
- `LogoutUsecaseTest`: 정상 revoke / 존재하지 않는 토큰 / 이미 revoke된 토큰
- `SecureRandomRefreshTokenGeneratorTest`: 유일성 (여러 번 호출 시 중복 없음) / 예측 불가 길이
- 기존 `LoginWithGoogleUsecaseTest` 수정 (Refresh 발행 assertion 추가)
- AuthController의 `/refresh`, `/logout` 통합 테스트는 시간 부족 시 MockMvc 간략 1~2개

예상 diff: ~500줄 (문서 포함)

## TDD 순서

1. Flyway V2 + RefreshTokenEntity
2. Port + Repository adapter
3. SecureRandomRefreshTokenGenerator + test
4. RefreshTokensUsecase + test (reuse 감지 명시)
5. LogoutUsecase + test
6. LoginWithGoogleUsecase 수정
7. Controller endpoints
8. `./gradlew test`

## 검증

```bash
cd backend && ./gradlew test
```

전체 테스트 통과. Integration test는 여전히 @Disabled (Rancher Desktop 이슈).
