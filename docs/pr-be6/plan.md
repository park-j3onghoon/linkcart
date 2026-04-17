# PR-BE6: SSRF 방어 강화

## 목적

BE2의 기본 SSRF 방어(SafeUrlChecker)에 우회 벡터 3종을 추가 차단하고, 이미지 프록시에서 SVG 렌더링 공격 벡터를 차단한다.

## 배경

BE2 plan NOT in scope로 분리된 항목:
- **DNS rebinding / URL-encoded 우회 / 짧은 IP 표기(decimal/hex)** — 추후 보안 PR
- **SVG 실제 차단** — 정책 변경 필요

이번 PR에서 실용 범위 내 우회 벡터 커버.

## 브랜치

`feature/pr-be6-ssrf-hardening` (origin/main 기준)

## 공격 벡터 분석

### 1. URL-encoded bypass
- 예: `http://%31%32%37%2e%30%2e%30%2e%31/` → decode 시 `http://127.0.0.1/`
- 현재 `URI(url).host`는 percent-encoded host를 illegal character로 인식하여 null 또는 원문 유지.
- 일부 조합은 `%` 포함 host로 통과 가능 — 방어 필요.

### 2. 짧은 IP 표기 (decimal / hex / octal)
- Decimal 32-bit: `http://2130706433/` = 127.0.0.1
- Hex: `http://0x7f000001/` = 127.0.0.1
- Octal: `http://0177.0.0.1/` = 127.0.0.1
- `InetAddress.getByName`은 이들을 정상 IP로 해석해주지만, 일부 우회 가능성 있어 **표기 형태 자체를 거부**.

### 3. SVG via image proxy
- 서버가 이미지 `Content-Type: image/svg+xml` 응답을 허용하면 SVG 내 `<script>` / XSS 벡터 전달.
- 현재 `contentType.type == "image"` 만 검사 → SVG 통과.

### 4. DNS rebinding (NOT in scope)
- Time-of-check / Time-of-use (TOCTOU) 갭 exploit.
- 완전 방어는 DNS 캐싱 정책, 실제 connection peer IP 검증, TLS 제약 등 복잡.
- **이번 PR에서 다루지 않음**. SafeUrlChecker 주석으로 명시.

## 변경 상세

### 1. `SafeUrlChecker.kt` 강화

```kotlin
@Component
class SafeUrlChecker {

    fun isSafe(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (uri.scheme !in ALLOWED_SCHEMES) return false

        val rawHost = uri.host ?: return false

        // URL-encoded bypass 방어: host에 % 포함 거부
        if (rawHost.contains('%')) return false

        // 짧은 IP 표기 (decimal/hex/octal) 거부
        if (isSuspiciousNumericHost(rawHost)) return false

        val address = runCatching { InetAddress.getByName(rawHost) }.getOrNull() ?: return false
        return !address.isAnyLocalAddress &&
            !address.isLoopbackAddress &&
            !address.isLinkLocalAddress &&
            !address.isSiteLocalAddress
    }

    private fun isSuspiciousNumericHost(host: String): Boolean {
        // 32-bit decimal (예: "2130706433")
        if (host.all { it.isDigit() }) return true
        // hex (예: "0x7f000001")
        if (host.startsWith("0x", ignoreCase = true)) return true
        // octal dotted (예: "0177.0.0.1")
        if (host.matches(Regex("^0\\d+(?:\\.\\d+){3}$"))) return true
        return false
    }

    companion object {
        private val ALLOWED_SCHEMES = setOf("http", "https")

        // NOT in scope: DNS rebinding 방어. 호스트 해석 시점과 실제 요청 시점 간
        // DNS 응답 변조로 IP가 바뀌는 TOCTOU 공격은 별도 대응 필요.
    }
}
```

### 2. `ImageProxyController.kt` SVG 차단

**방식 결정**: allow-list (subtype 명시 허용) vs deny-list (svg+xml만 거부)

- **allow-list 채택**: `png`/`jpeg`/`gif`/`webp` 등 명시. 미래에 새 이미지 포맷(avif, heic 등)이 나와도 명시 전까지 거부. 보안 기본값 "deny".

```kotlin
if (contentType == null || contentType.type != "image" ||
    contentType.subtype !in ALLOWED_IMAGE_SUBTYPES) {
    throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "지원하지 않는 이미지 형식입니다")
}

companion object {
    private val ALLOWED_IMAGE_SUBTYPES = setOf("png", "jpeg", "gif", "webp")
}
```

기존 `image/bmp` 처리 주의 — 현재 허용 중. allow-list에 포함시킬지 결정:
- 일반 쇼핑몰은 bmp 안 씀. 제외 안전.

## 테스트

### SafeUrlCheckerTest.kt 추가 케이스
- `url-encoded loopback returns false` — `http://%31%32%37%2e%30%2e%30%2e%31/`
- `percent-containing host returns false` — `http://foo%20bar.example.com`
- `decimal IP returns false` — `http://2130706433`
- `hex IP returns false` — `http://0x7f000001`
- `octal dotted IP returns false` — `http://0177.0.0.1`
- `normal dotted decimal IP returns true` — `http://8.8.8.8` (이미 있음, 회귀 확인)
- `normal hostname returns true` — `http://example.com` (회귀 확인)

### ImageProxyControllerTest.kt 추가 케이스
- `svg content type is blocked` — `image/svg+xml` → 502 (기존 `svg content type is allowed` 테스트 반전)
- `png content type is allowed` — `image/png` → 200 (positive 회귀)
- `bmp content type is blocked` — `image/bmp` → 502 (allow-list 정책 결정 반영)

기존 테스트 영향:
- BE2의 `svg content type is allowed` 테스트 삭제/반전 필요.

## NOT in scope

- DNS rebinding 방어 (TOCTOU, 복잡한 별도 작업)
- IPv6 short form bypass (`[::1]` 이미 방어됨, 다른 변형 추후)
- 이미지 실제 파일 매직 바이트 검증 (Content-Type만 신뢰 대신 magic bytes 검사는 별도)
- URL fragmented / double-encoding 심층 케이스
- Redirect chase의 최종 URL SSRF 검증 (302 리다이렉트 이후 IP 재검증)

## 보안 정책 문서

`SafeUrlChecker` companion 주석에 DNS rebinding을 다루지 않음을 명시하여 후속 작업자에게 알림.

## TDD 순서

1. `SafeUrlCheckerTest.kt` 5개 신규 케이스 작성 (RED)
2. `SafeUrlChecker.kt` 강화 (GREEN): `%` 거부 + `isSuspiciousNumericHost`
3. `ImageProxyControllerTest.kt` 케이스 수정/추가 (RED)
4. `ImageProxyController.kt` allow-list (GREEN)
5. `./gradlew test` 전체 통과

## 파일 변경

### 수정
- `backend/src/main/kotlin/com/linkcart/infrastructure/adapter/parser/SafeUrlChecker.kt` — `isSuspiciousNumericHost`, `%` 체크, DNS rebinding 주석
- `backend/src/main/kotlin/com/linkcart/presentation/api/ImageProxyController.kt` — allow-list subtype
- `backend/src/test/kotlin/com/linkcart/infrastructure/adapter/parser/SafeUrlCheckerTest.kt` — 5개 케이스 추가
- `backend/src/test/kotlin/com/linkcart/presentation/api/ImageProxyControllerTest.kt` — SVG 반전, PNG allow, BMP block

예상 diff: ~180줄

## 검증

```bash
cd backend && ./gradlew test
```

보안 강화된 거부 케이스 모두 false 반환. 기존 허용 케이스 true 유지.
