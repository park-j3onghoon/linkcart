# PR-P2-2: Spring Security + Google OAuth + Access JWT

## 목적

Google OAuth 로그인과 Access JWT 발행을 구현. SPA/앱이 Google Authorization Code를 BE로 보내면 BE가 Google과 교환 → User 찾기/생성 → Access JWT 반환.

## 브랜치

`feature/pr-p2-2-oauth-jwt` (origin/main 기준)

## OAuth 흐름 (Authorization Code, backend exchange)

```
FE/App ─ Google OAuth consent ─▶ Google
                                    │
                                    ▼
FE/App ◀── auth code ──────────── Google

FE/App ── POST /auth/oauth/google {code} ──▶ BE
                                              │
                                              ├─ Google token endpoint (code → id_token + access_token)
                                              ├─ ID Token 검증 (google-auth-library)
                                              ├─ UserRepository.findOrCreate
                                              └─ Access JWT 발행 (HS256, 15분)
                                              │
FE/App ◀── {access_token, user} ──────────── BE
```

모바일도 동일 API. 앱에서 Google SDK로 code 획득 후 동일 POST.

## 스코프

### 포함
- Spring Security 설정 (JWT stateless, CSRF off)
- `JwtAuthenticationFilter` (Bearer 토큰 검증, Access만)
- `GoogleOAuthAdapter` (code → id_token 교환 + 검증)
- `JwtAccessTokenIssuer` (HS256 발행)
- `FindOrCreateUserUsecase` (OAuth subject → User)
- `AuthController`:
  - `POST /api/v1/auth/oauth/google` (public)
  - `GET /api/v1/auth/me` (protected, JWT 필요)

### NOT in scope
- Refresh Token + rotation → P2-3
- 로그아웃 → P2-3
- Kakao/Apple → 별도 PR

## 주요 설계

### 엔드포인트

**POST /api/v1/auth/oauth/google**
- Request: `{ "code": "<google_auth_code>", "redirect_uri": "<used_in_FE_flow>" }`
- Response (200): `{ "access_token": "<jwt>", "token_type": "Bearer", "expires_in": 900, "user": { "id", "email", "display_name", "avatar_url", "provider": "google" } }`
- 오류:
  - 400 validation_error (code 빈 값)
  - 401 invalid_credentials (Google token 교환 실패)
  - 502 upstream_error (Google 서버 응답 실패)

**GET /api/v1/auth/me**
- Header: `Authorization: Bearer <jwt>`
- Response (200): `{ "user": { ... } }`
- 401: 토큰 없음/만료/서명 검증 실패

### JWT 형식

```
header.payload.signature (HS256)
payload = {
  "sub": "<userId>",          // 주체 = 내부 user.id (String)
  "iat": <epoch_seconds>,
  "exp": <epoch_seconds + 900>,
  "iss": "linkcart",
  "aud": "linkcart-api"
}
```

- `sub`을 `userId` (내부)로 두어 `/auth/me` 등 lookup 간편.
- Access token에는 provider/email 등 포함 안 함 (최소화, 민감정보 JWT 노출 금지).

### Spring Security 구성

```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfig {
    fun filterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers(
                "/api/v1/auth/oauth/**",
                "/api/v1/products/parse",
                "/api/v1/images/proxy",
                "/openapi.json", "/docs/**", "/swagger-ui/**",
            ).permitAll()
                .anyRequest().authenticated()
        }
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .exceptionHandling { it.authenticationEntryPoint(jsonAuthEntryPoint) }
        .build()
}
```

- 401 응답도 JSON 포맷 (`{"code": "unauthorized", "message": "..."}`)으로 통일.

### JwtAuthenticationFilter

Bearer 토큰 추출 → 서명 검증 → 만료 확인 → SecurityContext에 userId 주입.

### ID Token 검증

`google-auth-library` 사용. Google JWK 자동 fetch + 서명 검증.

### 의존성 (build.gradle.kts)

```kotlin
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("io.jsonwebtoken:jjwt-api:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")
```

### 환경 변수 (application.yml)

```yaml
linkcart:
  google:
    client-id: ${LINKCART_GOOGLE_CLIENT_ID}
    client-secret: ${LINKCART_GOOGLE_CLIENT_SECRET}
    token-url: ${LINKCART_GOOGLE_TOKEN_URL:https://oauth2.googleapis.com/token}
  jwt:
    secret: ${LINKCART_JWT_SECRET}  # HMAC-SHA256 32B+
    access-token-ttl-seconds: 900
    issuer: linkcart
    audience: linkcart-api
```

`.env.example`에도 반영.

## 파일 구조

```
domain/model/
  AccessToken.kt                          (value object)
  GoogleIdentity.kt                       (Google 응답에서 추출된 User 원본)

application/auth/
  port/GoogleOAuthClient.kt               (port)
  port/AccessTokenIssuer.kt               (port)
  usecase/LoginWithGoogleUsecase.kt       (OAuth 메인 흐름)

infrastructure/adapter/auth/
  GoogleOAuthAdapter.kt                   (Google token endpoint + ID Token 검증)
  JwtAccessTokenIssuer.kt                 (JJWT HMAC)
  JwtAuthenticationFilter.kt              (Bearer 검증)

infrastructure/config/
  SecurityConfig.kt                       (Filter chain, permitAll/authenticated)
  JsonAuthenticationEntryPoint.kt         (401 JSON 포맷)

presentation/api/
  AuthController.kt                       (/oauth/google, /me)

presentation/dto/
  OAuthLoginRequest.kt
  OAuthLoginResponse.kt
  UserResponse.kt
  MeResponse.kt
```

## TDD 순서

1. 의존성 추가 + application.yml + .env.example 수정
2. 포트: `GoogleOAuthClient`, `AccessTokenIssuer`
3. 도메인: `AccessToken`, `GoogleIdentity`
4. 어댑터: `GoogleOAuthAdapter` (RestTemplate), `JwtAccessTokenIssuer`
5. UseCase: `LoginWithGoogleUsecase`
6. 필터: `JwtAuthenticationFilter`
7. 보안 설정: `SecurityConfig`, `JsonAuthenticationEntryPoint`
8. DTO + `AuthController`
9. 테스트:
   - `LoginWithGoogleUsecaseTest` (find / create 분기, 재로그인, OAuth 실패)
   - `JwtAccessTokenIssuerTest` (발행 → 검증 round-trip, 만료, 잘못된 sub)
   - `AuthControllerTest` (MockMvc: 200 happy path, 401 invalid token, 400 blank code)
10. `./gradlew test` 전체 통과

## 파일 변경 요약

- 신규: ~13 파일 (config/dto/usecase/adapter/filter/controller/test)
- 수정: build.gradle.kts, application.yml, .env.example
- 테스트: @WebMvcTest + mocked UseCase/JwtFilter 조합으로 간결 유지

예상 diff: ~600줄 (WARNING 범위) — 문서 제외 시 ~450. Spring Security는 첫 도입이라 설정 많음.

## 검증

```bash
cd backend && ./gradlew test
./gradlew build
```

## 보안 주의

- JWT payload에 `sub`만 유지 (email/avatar 같은 PII는 DB 조회로)
- JWT secret 32바이트+ HMAC-SHA256, 환경변수 필수
- OAuth client_secret은 서버 전용
- Google ID Token 서명 검증 필수 (audience = client_id 확인)
- 401/400 에러 메시지는 사용자/로그 분리 (enumeration 방지)
