# PR-BE4: ParserNames → enum class ParserName

## 목적

파서 식별자를 String 상수 카탈로그에서 **enum 타입**으로 전환하여 타입 안전성을 확보한다.

- `ParseResult.parserUsed`가 임의 String을 허용 → 유효한 ParserName만 허용
- 오타/잘못된 이름이 컴파일 타임에 잡힘
- Jackson `@JsonValue`로 API 응답은 기존 String 형태 유지 (FE 호환)

## 브랜치

`feature/pr-be4-parser-name-enum` (origin/main 기준)

## 현재 구조

```kotlin
// application/parser/ParserNames.kt
object ParserNames {
    const val OG = "og"
    const val COUPANG = "coupang-api"
    const val ELEVENST = "11st-api"
}

// ParseResult.kt
sealed interface ParseResult {
    data class Success(... val parserUsed: String, ...)
    data class Partial(... val parserUsed: String, ...)
    data class Failure(val reason: String, val parserUsed: String)
}
```

문제: `parserUsed: String`은 임의 문자열 허용. 도메인 계약이 약함.

## 목표 구조

```kotlin
// application/parser/ParserName.kt
enum class ParserName(@JsonValue val code: String) {
    OG("og"),
    COUPANG("coupang-api"),
    ELEVENST("11st-api"),
}

// ParseResult.kt
sealed interface ParseResult {
    data class Success(... val parserUsed: ParserName, ...)
    data class Partial(... val parserUsed: ParserName, ...)
    data class Failure(val reason: String, val parserUsed: ParserName)
}
```

- `ParserNames` object 삭제.
- Jackson `@JsonValue`가 enum → `code` String 직렬화 (FE 입장 호환).
- 각 파서 companion의 `PARSER_NAME: String` → `PARSER_NAME: ParserName`.

## 변경 상세

### 1. `domain/model/ParserName.kt` 신규

```kotlin
package com.linkcart.domain.model

import com.fasterxml.jackson.annotation.JsonValue

enum class ParserName(@JsonValue val code: String) {
    OG("og"),
    COUPANG("coupang-api"),
    ELEVENST("11st-api"),
    ;

    override fun toString(): String = code
}
```

**근거**:
- 위치: `domain/model/` — `ParseResult.parserUsed: ParserName`이 도메인 계약이므로 domain에 속함. 단방향 의존성 원칙에 따라 자연스러운 귀결.
- `@JsonValue` domain 의존: `domain/vo/Mall.kt`가 이미 `@JsonValue` 사용 중 (팀 관행 선례). Rule of Three 관점 타협 가능. Jackson Module 분리는 YAGNI.
- `toString() = code` 오버라이드: 에러 메시지/로그/문자열 템플릿에서 `${parserUsed}`가 자연스럽게 `"og"` 반환. API 응답과 동일 문자열 → grep/디버깅 일관성.

### 2. `application/parser/ParserNames.kt` 삭제

### 3. `domain/model/ParseResult.kt` 시그니처 변경

```kotlin
data class Success(val product: Product, val parserUsed: ParserName, val fallbackUsed: Boolean = false)
data class Partial(val fields: Map<String, Any>, val parserUsed: ParserName, val fallbackUsed: Boolean = false)
data class Failure(val reason: String, val parserUsed: ParserName)
```

- `domain/model`에서 `application/parser/ParserName`을 참조하는 건 **레이어 역방향**. domain이 application을 알면 안 됨.
- 해결: `ParserName`을 `domain/model/ParserName.kt`로 배치. 도메인 개념이 됨 ("파싱 결과에 사용된 파서 식별자").
- 주의: BE3 plan-review Issue 3에서 "파서명은 맥락 의존적 → application"이라 했으나, **타입으로 격상되면 ParseResult의 일부이므로 domain에 속함**. layer placement criteria 재해석.

### 4. `ParserName` 위치 최종 결정: `domain/model/ParserName.kt`

- `ParseResult.parserUsed: ParserName` → ParseResult는 domain → ParserName도 domain.
- BE3는 String 상수였기에 "어떤 파서가 식별됐는지 레이블"이 application context였음.
- BE4에서 타입화되며 도메인 모델의 일부가 됨.

### 5. 각 파서 companion `PARSER_NAME` 제거

기존 `const val PARSER_NAME = ParserNames.OG` → enum 전환으로 래핑층 불필요. 직접 `ParserName.OG` 사용.

```kotlin
// OgParser.kt 내부
return ParseResult.Failure(reason = "허용되지 않는 URL입니다", parserUsed = ParserName.OG)
```

- companion `PARSER_NAME` 삭제.
- 각 파서의 parserUsed 참조는 `ParserName.XXX` 직접.
- 외부(테스트 등)에서 `OgParser.PARSER_NAME`에 접근하던 참조도 `ParserName.OG`로 교체.

### 6. `ParseProductUseCase.kt` 에러 메시지

```kotlin
reason = "전용 파서(${primaryResult.parserUsed}) 실패: ${primaryResult.reason}; 폴백(${fallbackResult.parserUsed}) 실패: ${fallbackResult.reason}"
```

- `toString() = code` 오버라이드 덕분에 `${parserUsed}` 자체가 `"og"` 반환. `.code` 명시 불필요.

### 7. `presentation/dto/ParseResponse.kt`

```kotlin
data class ParseResponse(
    ...
    val parserUsed: ParserName,  // String → ParserName
    ...
)
```

- Jackson이 `@JsonValue` 덕분에 `code` 문자열로 직렬화. FE 응답 형식 변화 없음.
- `domain/vo/Mall.kt`가 이미 동일 패턴 사용. 일관성 유지.

### 8. 테스트 전체 업데이트

- **타입 시그니처 변경**: `assertEquals("og", result.parserUsed)` → `assertEquals(ParserName.OG, result.parserUsed)`.
- **테스트 stub ParserName**: 기존 `"stub"`/`"test"` 임시 더미는 해당 테스트 맥락에 맞는 ParserName 선택.
  - ParserResolverTest의 dedicated stub: 라우팅 로직만 검증하므로 `ParserName.OG` 재활용 OK (plan에 기록).
  - UseCaseTest dedicated/fallback 역할 구분 필요한 곳: 명확한 값 (`COUPANG`, `OG`)로.
- **ProductControllerTest MockMvc assertion**: `jsonPath("$.parser_used").value("coupang-api")` **하드코드 문자열 유지**. enum 경유 시 enum 정의 오타를 감지 못함.
- **given() stub의 `parserUsed` 입력은 enum으로 전환**: `parserUsed = ParserName.COUPANG`. input(enum) ↔ output assertion(하드코드 문자열) 분리로 `@JsonValue` 회귀 감지.

### 9. ProductControllerTest에 11st-api MockMvc 케이스 추가 (Issue 24)

현재 `parser_used` assertion은 `"coupang-api"`, `"og"` 2개 값만 검증. `"11st-api"` 계약 커버리지 부재 → enum `ELEVENST("11st-api")` 오타 시 감지 불가.

```kotlin
@Test
fun `successful 11st parse returns 200 with parser_used 11st-api`() {
    // given: ParseProductUseCase mock → Success with parserUsed = ParserName.ELEVENST
    // when: POST /api/v1/products/parse with 11st URL
    // then: status 200, jsonPath("$.parser_used").value("11st-api")
}
```

- 11st 성공 케이스 1개 추가로 3개 enum code 모두 JSON 계약 박제.

## NOT in scope

- 파서 추가/삭제
- Fallback 정책 응집 (BE5)
- SSRF 방어 강화 (BE6)
- 에러 메시지 상수화
- Mall enum 등 다른 도메인 모델 변경

## TDD 순서 (리팩토링)

1. `ParserName` enum 신규 (domain/model/)
2. `ParseResult` sealed interface의 parserUsed 타입 변경 → 컴파일 에러 체인 확인
3. 각 파서 companion PARSER_NAME 타입 변경
4. `ParseProductUseCase` 에러 메시지 `.code` 접근
5. `ParseResponse` parserUsed 타입 변경
6. `ParserNames` object 삭제
7. 테스트 전체 업데이트 (문자열 → enum)
8. `./gradlew test` 통과 확인
9. `./gradlew build` 통과 확인

## 파일 변경

### 신규 (+1)
- `backend/src/main/kotlin/com/linkcart/domain/model/ParserName.kt` (enum + @JsonValue + toString override)

### 삭제 (-1)
- `backend/src/main/kotlin/com/linkcart/application/parser/ParserNames.kt`

### 수정
- `backend/src/main/kotlin/com/linkcart/domain/model/ParseResult.kt`
- `backend/src/main/kotlin/com/linkcart/application/usecase/ParseProductUseCase.kt`
- `backend/src/main/kotlin/com/linkcart/presentation/dto/ParseResponse.kt`
- `backend/src/main/kotlin/com/linkcart/infrastructure/adapter/parser/OgParser.kt`
- `backend/src/main/kotlin/com/linkcart/infrastructure/adapter/parser/CoupangParser.kt`
- `backend/src/main/kotlin/com/linkcart/infrastructure/adapter/parser/ElevenStParser.kt`
- `backend/src/test/kotlin/com/linkcart/presentation/api/ProductControllerTest.kt` (11st 성공 케이스 추가)
- 각종 테스트 파일 (문자열 → enum 참조 전환, MockMvc assertion은 하드코드 유지)

예상 diff: ~180줄 실변경 (11st MockMvc 테스트 추가 포함)

## 검증

```bash
cd backend && ./gradlew test
./gradlew build
```

- 기존 테스트 모두 통과 (값 일치 보장)
- FE 응답 JSON의 `parser_used` 필드는 기존 문자열 형태 유지 (MockMvc 테스트로 검증)
