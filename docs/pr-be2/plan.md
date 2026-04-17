# PR-BE2: 컨트롤러/SafeUrlChecker 단위 테스트

## 목적

SafeUrlChecker SSRF 방지 로직의 단위 테스트를 신규 도입하고, 컨트롤러의 엣지 케이스 공백을 메운다. 프로덕션 코드 변경 0.

## 브랜치

`feature/pr-be2-controller-safeurl-tests` (origin/main 기준)

## 테스트 13개 (plan-review 반영 후)

### SafeUrlCheckerTest.kt (신규 파일, +9)

1. `valid external url returns true` — `https://8.8.8.8`
2. `javascript scheme returns false` — `javascript:alert(1)`
3. `uppercase scheme returns false` — `HTTP://example.com` (소문자만 허용 정책 고정)
4. `IPv4 loopback returns false` — `http://127.0.0.1`
5. `IPv6 loopback returns false` — `http://[::1]`
6. `any local address returns false` — `http://0.0.0.0`
7. `link local address returns false` — `http://169.254.1.1`
8. `private IP returns false` — `http://10.0.0.1` (대표 1개, isSiteLocalAddress)
9. `malformed URL returns false` — URI 파싱 불가 URL

### ProductControllerTest.kt (+2)

10. `whitespace-only url returns 400` — `"   "` → @NotBlank 실패
11. `javascript protocol returns 400` — `"javascript:alert(1)"` → @Pattern 실패

### ImageProxyControllerTest.kt (+2)

12. `null content type returns 502` — upstream 응답 Content-Type 헤더 없음
13. `svg content type is allowed` — `image/svg+xml` → 200 (현재 `type == image` 정책)

## plan-review 반영 규칙

### Issue 17: 테스트명 통일 (BE1 컨벤션)
- `accepts valid external https url` → `valid external url returns true`
- `rejects X` → `X returns false`
- 모든 테스트명을 `subject 동사 result` 구조로 통일.

### Issue 18: scheme 전수검증 축소
- 원 계획의 `rejects non-http scheme (ftp/javascript/file)` → `javascript scheme returns false` 1개로.
- 이유: whitelist(`setOf("http", "https")`) 기반 구현이라 대표 1개로 검증 충분. 무효 케이스 전수검증 불필요.

### Issue 19: private IP range 축소
- 원 계획의 `rejects private IP ranges (10.x/172.16.x/192.168.x)` → `private IP returns false` 1개로.
- 이유: `InetAddress.isSiteLocalAddress` 판별 로직은 프레임워크 영역. 대표 1개로 "우리 코드가 호출하는지"만 검증.

### Issue 20: companion object 상수화
- SafeUrlCheckerTest.kt 내 상수:
  ```kotlin
  companion object {
      private const val VALID_EXTERNAL = "https://8.8.8.8"
      private const val JAVASCRIPT_URL = "javascript:alert(1)"
      private const val UPPERCASE_URL = "HTTP://example.com"
      private const val LOOPBACK_V4 = "http://127.0.0.1"
      private const val LOOPBACK_V6 = "http://[::1]"
      private const val ANY_LOCAL = "http://0.0.0.0"
      private const val LINK_LOCAL = "http://169.254.1.1"
      private const val PRIVATE_IP = "http://10.0.0.1"
      private const val MALFORMED = "not a url"
  }
  ```
- ControllerTest 기존 companion object(있으면)에 `WHITESPACE_URL`, `JAVASCRIPT_URL` 추가.

### Security 이슈 (9~12)
- SSRF 바이패스 벡터 커버: IPv6 `::1`, `0.0.0.0`, link-local `169.254.x.x`, 대문자 scheme.
- `SafeUrlChecker.isSafe`의 모든 주요 분기(isAnyLocal/isLoopback/isLinkLocal/isSiteLocal)를 직접 검증.

## NOT in scope

- **DNS rebinding / URL-encoded 우회 / 짧은 IP 표기(decimal/hex)** — 현재 구현이 방어하지 않음. SSRF 방어 강화는 별도 보안 PR.
- **SVG 실제 차단** — 정책 변경 필요. 현재는 "image/* 통과" 정책을 테스트로 고정.
- **DNS 조회 실패 분기** — 환경 의존(네트워크 상태)으로 테스트가 플래키해짐.
- **프로덕션 코드 변경** — 테스트 커버리지만 추가.

## What already exists

- ProductController 6개: successful / blank / invalid ftp / parse failure / partial / unsafe
- ImageProxyController 5개: successful / blank / unsafe / fetch failure / non-image
- SafeUrlChecker: 단위 테스트 없음, 컨트롤러에서 mock으로만 간접 호출

## Failure modes

- 모든 신규 13개 테스트가 `isSafe()` 반환값 / HTTP status + code를 구체적으로 assert
- 플래키 케이스 없음 (IP 주소 직접 사용으로 DNS 회피)

## TDD 순서

1. `SafeUrlCheckerTest.kt` 신규 (+9 케이스, companion object 상수) → `./gradlew test --tests SafeUrlCheckerTest` 통과
2. `ProductControllerTest.kt` +2 → `./gradlew test --tests ProductControllerTest` 통과
3. `ImageProxyControllerTest.kt` +2 → `./gradlew test --tests ImageProxyControllerTest` 통과
4. `./gradlew test` 전체 통과

## 파일 변경

- `backend/src/test/kotlin/com/linkcart/infrastructure/adapter/parser/SafeUrlCheckerTest.kt` (신규)
- `backend/src/test/kotlin/com/linkcart/presentation/api/ProductControllerTest.kt`
- `backend/src/test/kotlin/com/linkcart/presentation/api/ImageProxyControllerTest.kt`

예상 diff: ~250줄

## Completion summary

- Step 0: BIG CHANGE (4 Agent 중 Coding Standards 1개 수신 + Architecture/Security/Test Coverage 직접 검토, rate limit)
- Dimensions: 4/6 active
- Architecture: 0 / Security: 4 / Coding Standards: 4 / Test Coverage: 0 overlap
- Critical gaps: 0
