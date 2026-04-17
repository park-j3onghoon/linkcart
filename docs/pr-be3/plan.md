# PR-BE3: OgParser/ParserFactory 아키텍처 명확화

## 목적

파서 라우팅/폴백 구조의 모호함을 제거하여 아키텍처를 명확하게 한다.

- `ProductParser` 포트의 라우팅(`canParse`) 책임과 파싱(`parse`) 책임을 타입으로 분리
- `ParserFactory`에서 인스턴스 동일성(`===`) / Bean 이름(`@Qualifier("ogParser")`) 의존 제거
- `XxxApiClient` / `XxxParser` 네이밍 혼재 해소
- `PARSER_NAME` 상수 카탈로그화

**기능/동작 변경 없음. 순수 구조/네이밍 리팩토링.**

## 브랜치

`feature/pr-be3-parser-architecture` (origin/main 기준)

## 현재 구조의 문제

1. `ProductParser.canParse(url)` — OgParser는 항상 `true` 반환. 라우팅 책임과 파싱 책임이 한 포트에 섞여 있어, 폴백 구분이 불명확.
2. `ParserFactory.parsers.filter { it !== fallbackParser }` — 인스턴스 동일성 비교로 폴백 제외. `@Qualifier("ogParser")` Bean name 문자열 의존.
3. `ParseProductUseCase.primaryParser === fallbackParser` — 같은 인스턴스 동일성 비교 중복.
4. `OgParser.PARSER_NAME = "og"` vs Bean name `"ogParser"` — 이중 이름.
5. `CoupangApiClient`, `ElevenStApiClient`는 `ProductParser` 구현체이므로 **Parser**가 역할인데 이름은 **ApiClient**.
6. PARSER_NAME 상수가 각 파서 companion에 흩어져 있어 전체 카탈로그를 한눈에 볼 수 없음.

## 목표 구조

```
domain/port/
  ProductParser.kt              (공통 포트 — parse만)
  DedicatedProductParser.kt     (canParse 추가, 전용 파서 마커)
  FallbackProductParser.kt      (폴백 파서 마커 — Spring DI discriminator 역할)

application/parser/
  ParserNames.kt                (PARSER_NAME 카탈로그 — 맥락 의존적 상수)
  ParserResolver.kt             (구 ParserFactory, 타입 기반 주입)

infrastructure/adapter/parser/
  OgParser.kt                   (FallbackProductParser 구현)
  CoupangParser.kt              (구 CoupangApiClient, DedicatedProductParser 구현)
  ElevenStParser.kt             (구 ElevenStApiClient, DedicatedProductParser 구현)
```

## 변경 상세

### 1. 포트 분리

```kotlin
// domain/port/ProductParser.kt
interface ProductParser {
    fun parse(url: String): ParseResult
}

// domain/port/DedicatedProductParser.kt
interface DedicatedProductParser : ProductParser {
    fun canParse(url: String): Boolean
}

// domain/port/FallbackProductParser.kt
interface FallbackProductParser : ProductParser
```

- `DedicatedProductParser`만 `canParse` 보유. `OgParser.canParse() = true` 같은 의미 없는 오버라이드 제거.
- **FallbackProductParser 마커 유지 이유**: Spring DI 타입 기반 주입에서 `ProductParser` 타입 빈이 3개(OG/Coupang/11st)가 되므로 ambiguous 방지용 discriminator 필요.
- Spring이 `List<DedicatedProductParser>` / `FallbackProductParser` 타입으로 주입.

### 2. PARSER_NAME 카탈로그

```kotlin
// application/parser/ParserNames.kt
object ParserNames {
    /** Open Graph protocol 기반 범용 파서 */
    const val OG = "og"
    const val COUPANG = "coupang-api"
    const val ELEVENST = "11st-api"

    // 후속 PR 후보: String → enum class ParserName 전환 (ParseResult.parserUsed 시그니처 변경 포함)
}
```

- 위치: `application/parser/` — 파서 식별자는 "어떤 파서가 사용됐는지를 식별하는 라우팅/운영용 상수"이므로 맥락 의존적 → application 레이어 (사용자 선호 `feedback_layer_placement_criteria` 반영).
- 각 파서 companion의 `PARSER_NAME`은 `ParserNames.XXX` 참조로 변경.

### 3. ParserFactory → ParserResolver

```kotlin
// application/parser/ParserResolver.kt
@Component
class ParserResolver(
    private val dedicatedParsers: List<DedicatedProductParser>,
    val fallbackParser: FallbackProductParser,  // public val — Kotlin 관용구, get()= 함수 대신 프로퍼티
) {
    fun resolve(url: String): ProductParser =
        dedicatedParsers.firstOrNull { it.canParse(url) } ?: fallbackParser
}
```

- 이름: `Factory`(생성 책임 뉘앙스) → `Resolver`(선택 책임 뉘앙스).
- 메서드: `getParser(url)` → `resolve(url)`.
- `filter { it !== fallbackParser }` 제거. 타입 분리로 폴백은 애초에 `dedicatedParsers`에 없음.
- `@Qualifier("ogParser")` 제거. 타입 기반 주입.
- `getFallback()` 함수 대신 public val `fallbackParser` 프로퍼티 노출 (Kotlin idiom).

### 4. UseCase 정리

```kotlin
// application/usecase/ParseProductUseCase.kt
@Service
class ParseProductUseCase(
    private val parserResolver: ParserResolver,
) {
    @Cacheable(...)
    fun execute(url: String): ParseResult {
        val primaryParser = parserResolver.resolve(url)
        val primaryResult = primaryParser.parse(url)

        // primary가 폴백 자체이면 재시도 의미 없음 — 단락.
        if (primaryResult !is ParseResult.Failure || primaryParser is FallbackProductParser) {
            return primaryResult
        }

        val fallbackParser = parserResolver.fallbackParser
        val fallbackResult = fallbackParser.parse(url)
        return when (fallbackResult) { ... }
    }
}
```

- `primaryParser === fallbackParser` → `primaryParser is FallbackProductParser`.
- 주석 한 줄로 판별 의도 명시.

### 5. 파서 클래스 이름 변경

| Before | After |
|---|---|
| `CoupangApiClient` | `CoupangParser` |
| `ElevenStApiClient` | `ElevenStParser` |
| `CoupangApiClientTest` | `CoupangParserTest` |
| `ElevenStApiClientTest` | `ElevenStParserTest` |

- `OgParser`는 이미 Parser suffix이므로 그대로.
- `git mv`로 rename하여 git history 보존.
- 클래스명/interface 변경은 rename 이후 같은 커밋에서.

### 6. Bean name 제거

- `OgParser`: `@Component("ogParser")` → `@Component`.
- `@Qualifier("ogParser")` 전부 삭제.
- 타입(`FallbackProductParser`)이 유일하므로 이름 불필요.

### 7. 테스트 stub 헬퍼 공통화

포트 분리로 기존 `object : ProductParser { override fun canParse ...; override fun parse ... }` 패턴 8곳이 컴파일 실패. 공통 헬퍼로 묶는다.

```kotlin
// test/.../parser/ParserTestStubs.kt (신규, 테스트 유틸)
internal fun dedicatedStub(
    canParse: (String) -> Boolean,
    parse: (String) -> ParseResult,
): DedicatedProductParser = object : DedicatedProductParser {
    override fun canParse(url: String) = canParse(url)
    override fun parse(url: String) = parse(url)
}

internal fun fallbackStub(
    parse: (String) -> ParseResult,
): FallbackProductParser = object : FallbackProductParser {
    override fun parse(url: String) = parse(url)
}
```

- ParserResolverTest, ParseProductUseCaseTest 양쪽에서 공유.
- 사용자 선호 `feedback_test_dedup_and_helpers` (중복 테스트 통합, 헬퍼 공통 위치) 반영.

### 8. ParseProductUseCaseCachingTest: CountingProductParser 타입 분리

현재 단일 `CountingProductParser`가 Bean name `"ogParser"` / `"primaryParser"`로 구분되는데, 타입 기반 DI로 전환하려면 **두 클래스**로 분리 필요.

```kotlin
// CachingTest 내부
private abstract class CountingParser : /* counting 로직 */ { ... }
private class DedicatedCountingParser(...) : CountingParser(), DedicatedProductParser { ... }
private class FallbackCountingParser(...) : CountingParser(), FallbackProductParser { ... }
```

- 공통 counting 로직은 추상 base로 추출.

### 9. UseCaseTest: `is FallbackProductParser` 회귀 테스트 추가

현재 테스트는 모두 dedicated가 매칭되는 URL로 검증하여, "primary가 폴백 자체일 때 parse가 1회만 호출됨"을 커버하지 않음.

```kotlin
@Test
fun `when primary is fallback, fallback parse is called only once`() {
    // dedicatedParsers = emptyList, fallbackParser = countingFallback
    // execute(url) → fallback.parse 호출 수 == 1
}
```

### 10. ParserResolverTest: `unknown domain` 케이스 재작성

기존 `ParserFactory(listOf(ogParser), ogParser)`는 OgParser를 dedicated 리스트에도 포함시켜 검증했으나, 신구조에서는 `List<DedicatedProductParser>`이므로 `FallbackProductParser`인 OgParser가 들어갈 수 없음.

- `ParserResolver(emptyList(), ogParser)` 형태로 재작성하여 "dedicated 없음 → fallback 반환" 의도를 신구조 의미에 맞게 표현.

## NOT in scope

- 기능/동작 변경 (DNS rebinding 방어, SVG 차단, 에러 메시지 상수화 등 전부 별도 PR)
- **fallback 정책 응집** — `parseWithFallback(url): ParseResult` 같이 폴백 실행 정책을 Resolver/Pipeline에 응집하는 작업은 별도 PR (현 PR은 인스턴스 비교/Bean 이름 제거에 집중, blast radius 제한).
- `ParserNames` → `enum class ParserName` 전환 + `ParseResult.parserUsed` 타입 변경 — 시그니처 전파 큼, 별도 PR.
- ParserResolver/UseCase 알고리즘 변경 (폴백 순서, 재시도 등)
- 파서 추가/삭제
- 애플리케이션 레이어 vs 인프라 레이어 재배치 (`application/parser` 위치 유지)

## TDD 순서

리팩토링이라 기존 테스트가 모두 통과해야 한다. TDD가 아닌 REFACTOR 순서로 진행하되, 각 단계 후 테스트 실행.

1. **포트 추가** — `ProductParser`에서 `canParse` 제거, `DedicatedProductParser` / `FallbackProductParser` 신규. `ParserNames` 추가(`application/parser/`).
2. **구현 전환** — OgParser는 `FallbackProductParser`, Coupang/11st는 `DedicatedProductParser`. `@Component("ogParser")` 제거. PARSER_NAME 참조를 `ParserNames`로.
3. **ParserResolver 도입** — `ParserFactory.kt` → `ParserResolver.kt`로 `git mv` 후 내용 수정 (public val fallbackParser). 호출부(UseCase) 업데이트.
4. **UseCase `is` 판별 + 회귀 테스트** — `===` 제거 + `is FallbackProductParser` 적용. `primary=fallback when dedicated is empty` 회귀 테스트 추가.
5. **테스트 stub 헬퍼** — `ParserTestStubs.kt` 신규. 기존 `object : ProductParser` 8곳을 `dedicatedStub()` / `fallbackStub()`로 교체.
6. **ResolverTest 재작성** — `emptyList()` 기반 케이스로.
7. **CachingTest 타입 분리** — `CountingProductParser` → `DedicatedCountingParser` + `FallbackCountingParser`.
8. **파서 클래스 rename** — `git mv CoupangApiClient.kt → CoupangParser.kt` + 클래스명/참조 변경. 11st 동일. 테스트 파일도 rename.
9. **전체 테스트 실행** — `./gradlew test` 통과 확인.
10. **전체 빌드** — `./gradlew build` 통과 확인.

각 단계 후 빌드 확인 (`./gradlew build -x test`).

## 파일 변경

### 신규 (+5)
- `backend/src/main/kotlin/com/linkcart/domain/port/DedicatedProductParser.kt`
- `backend/src/main/kotlin/com/linkcart/domain/port/FallbackProductParser.kt`
- `backend/src/main/kotlin/com/linkcart/application/parser/ParserNames.kt`
- `backend/src/test/kotlin/com/linkcart/infrastructure/adapter/parser/ParserTestStubs.kt` (테스트 stub 공통 헬퍼)

### 수정
- `backend/src/main/kotlin/com/linkcart/domain/port/ProductParser.kt` — canParse 제거
- `backend/src/main/kotlin/com/linkcart/infrastructure/adapter/parser/OgParser.kt` — interface 변경, Bean name 제거, PARSER_NAME 참조
- `backend/src/main/kotlin/com/linkcart/application/usecase/ParseProductUseCase.kt` — is 판별, 주석, fallbackParser 프로퍼티 접근
- `backend/src/test/kotlin/com/linkcart/infrastructure/adapter/parser/OgParserTest.kt` — canParse 관련 테스트 삭제
- `backend/src/test/kotlin/com/linkcart/application/usecase/ParseProductUseCaseTest.kt` — Factory → Resolver 주입, stub 헬퍼 사용, `is FallbackProductParser` 회귀 테스트 추가
- `backend/src/test/kotlin/com/linkcart/application/usecase/ParseProductUseCaseCachingTest.kt` — CountingProductParser 타입 분리, 타입 기반 주입

### Rename (git mv)
- `application/parser/ParserFactory.kt` → `application/parser/ParserResolver.kt` (+ 내용 수정)
- `infrastructure/adapter/parser/CoupangApiClient.kt` → `CoupangParser.kt` (+ 클래스명 변경)
- `infrastructure/adapter/parser/ElevenStApiClient.kt` → `ElevenStParser.kt` (+ 클래스명 변경)
- `test/.../ParserFactoryTest.kt` → `ParserResolverTest.kt` (+ `emptyList()` 케이스 재작성)
- `test/.../CoupangApiClientTest.kt` → `CoupangParserTest.kt`
- `test/.../ElevenStApiClientTest.kt` → `ElevenStParserTest.kt`

예상 diff: ~300줄 (rename detection 제외 실 변경 기준)

## 검증

```bash
cd backend && ./gradlew test
# 모든 기존 테스트 + 리팩토링된 테스트 + 신규 회귀 테스트 PASS
./gradlew build
# 빌드 성공
```

## plan-review 반영 (Step 0: BIG CHANGE)

- Dimensions: 3/6 active (Architecture, Coding Standards, Test Coverage)
- Architecture: 4 이슈 (1 기각 Spring DI 제약, 3 반영)
- Coding Standards: 4 이슈 (1 연계 기각, 3 반영)
- Test Coverage: 4 이슈 (4 반영)
- Critical gaps: 0

### 반영 내역
- NOT in scope 에 "fallback 정책 응집" 명시
- `ParserNames` 위치: `domain/model/` → `application/parser/`
- `ParserNames` 주석: OG는 Open Graph, 후속 enum화 가능성
- `ParserResolver.getFallback()` 함수 → `val fallbackParser` public 프로퍼티
- UseCase `is FallbackProductParser` 판별에 의도 주석
- 테스트 stub 공통 헬퍼 (`ParserTestStubs.kt`)
- `is FallbackProductParser` 단락 경로 회귀 테스트 추가
- `ParserResolverTest` `unknown domain` 케이스 `emptyList()` 재작성
- `CachingTest`의 `CountingProductParser` 타입 분리

### 기각 내역
- Arch Issue 1 (옵션 B: FallbackProductParser 마커 제거 후 OgParser가 ProductParser 직접 구현) — Spring DI에서 `ProductParser` 타입 빈이 3개가 되어 ambiguous. 마커 interface가 필수 discriminator.
