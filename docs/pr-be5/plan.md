# PR-BE5: Fallback 정책 응집 (ParserPipeline 도입)

## 목적

`ParseProductUseCase`에 흩어진 폴백 실행 정책을 **`ParserPipeline`**에 응집한다. UseCase는 파이프라인 호출과 캐싱만 담당 → SRP 개선.

## 배경

BE3 plan-review Issue 2 / BE3 리뷰 Agent 3 Issue 2에서 제기됨.

현재 `ParseProductUseCase.execute`가 하는 일:
1. `parserResolver.resolve(url)` — primary 파서 선택
2. `primaryParser.parse(url)` — primary 실행
3. 단락 판별 (primary=fallback OR Success/Partial)
4. `parserResolver.fallbackParser.parse(url)` — fallback 실행
5. Success/Partial에 `fallbackUsed = true` 세팅
6. Failure 시 primary/fallback reason 조합 에러 메시지
7. `@Cacheable`로 캐싱

**문제**: UseCase가 "파서 오케스트레이션" + "캐싱 경계" 두 역할. 1~6 로직을 별도 컴포넌트로 분리하면 테스트 용이 + 후속 정책 변경(재시도, 다중 폴백 등) 시 Pipeline만 수정.

## 브랜치

`feature/pr-be5-parser-pipeline` (origin/main 기준)

## 목표 구조

```
application/parser/
  ParserResolver.kt         (선택만 담당 — 그대로)
  ParserPipeline.kt         (신규: 폴백 실행 정책 응집)

application/usecase/
  ParseProductUseCase.kt    (슬림화: 캐싱 + 파이프라인 호출만)
```

## 변경 상세

### 1. `application/parser/ParserPipeline.kt` 신규

```kotlin
package com.linkcart.application.parser

import com.linkcart.domain.model.ParseResult
import com.linkcart.domain.port.FallbackProductParser
import org.springframework.stereotype.Component

@Component
class ParserPipeline(
    private val parserResolver: ParserResolver,
) {
    fun parseWithFallback(url: String): ParseResult {
        val primaryParser = parserResolver.resolve(url)
        val primaryResult = primaryParser.parse(url)

        // primary가 폴백 자체이면 재시도 의미 없음 — 단락.
        if (primaryResult !is ParseResult.Failure || primaryParser is FallbackProductParser) {
            return primaryResult
        }

        val fallbackResult = parserResolver.fallbackParser.parse(url)
        return when (fallbackResult) {
            is ParseResult.Success -> fallbackResult.copy(fallbackUsed = true)
            is ParseResult.Partial -> fallbackResult.copy(fallbackUsed = true)
            is ParseResult.Failure -> ParseResult.Failure(
                reason = "전용 파서(${primaryResult.parserUsed.code}) 실패: ${primaryResult.reason}; 폴백(${fallbackResult.parserUsed.code}) 실패: ${fallbackResult.reason}",
                parserUsed = fallbackResult.parserUsed,
            )
        }
    }
}
```

### 2. `application/usecase/ParseProductUseCase.kt` 슬림화

```kotlin
@Service
class ParseProductUseCase(
    private val parserPipeline: ParserPipeline,
) {
    @Cacheable(
        cacheNames = ["products"],
        key = "#url",
        unless = "#result instanceof T(com.linkcart.domain.model.ParseResult\$Failure)",
    )
    fun execute(url: String): ParseResult = parserPipeline.parseWithFallback(url)
}
```

- 생성자 주입: `ParserResolver` → `ParserPipeline`.
- `execute()`는 위임만.

### 3. 테스트 재배치

**기존 `ParseProductUseCaseTest`의 파서 오케스트레이션 테스트 → `ParserPipelineTest`로 이동**:
- `dedicated parser success returns primary result`
- `dedicated failure falls back to OG success`
- `dedicated failure falls back to OG partial`
- `dedicated and fallback failures are combined`
- `timeout from dedicated parser still uses fallback`
- `when primary is fallback, fallback parse is called only once`

→ 이 테스트들은 `ParserPipeline(ParserResolver(...))`를 구성해 검증.

**`ParseProductUseCaseTest`에 남을 것**:
- 이 UseCase가 사실상 단순 위임이 됨. 단독 테스트 불필요 (사용자 선호: 단순 조합 UseCase 단독 테스트 불필요).
- 기존 `ParseProductUseCaseTest.kt`는 삭제하거나, 캐싱 무관 얇은 "caching 없이 호출 시 파이프라인 결과 반환" 스모크 1건만 남긴다.

**`ParseProductUseCaseCachingTest`**: 유지 (UseCase의 `@Cacheable` 동작 검증). 내부 구조 변경 필요:
- `TestApp`에 `ParserPipeline::class` 추가 @Import
- UseCase가 ParserPipeline을 주입받으므로 pipeline bean 필요

## NOT in scope

- `ParserResolver`의 추가 메서드/네이밍 변경 (이름/`resolve(url)` 그대로)
- 재시도 로직, 다중 폴백, 서킷 브레이커 등 고급 정책
- SSRF 방어 강화 (BE6)
- 에러 메시지 상수화
- 파서 validation 파라미터라이즈드 통합

## 설계 고려

### ParserPipeline vs ParserResolver 확장?

- **선택지 A**: `ParserResolver.parseWithFallback(url)` 메서드 추가 — Resolver가 선택+실행 양쪽 책임
- **선택지 B**: 별도 `ParserPipeline` 도입 — Resolver는 선택만, Pipeline이 실행

**B 채택 이유**: SRP 명확. "Resolver"라는 이름은 "적절한 것을 찾는다"는 선택 역할이 맞고, 실행까지 포함시키면 명명 혼란. Pipeline은 "주어진 리소스를 순차/조건부로 실행하는 흐름"의 의미.

### UseCase 슬림화의 회귀 리스크

- `execute()`가 단순 위임이 되면 `ParseProductUseCase`가 존재 의의가 약해 보일 수 있음.
- 하지만 **캐싱 경계**는 여전히 UseCase가 담당 (Spring `@Cacheable`의 AOP 프록시 위치).
- 향후 권한 검사, 로깅, 트랜잭션 등 횡단 관심사가 붙을 자리로 남겨둔다.

## TDD 순서 (리팩토링)

1. `ParserPipelineTest` 신규 — 기존 UseCaseTest 6개 케이스를 이동 + 컴파일
2. `ParserPipeline` 구현 (UseCase 로직 이식)
3. `ParseProductUseCase` 슬림화
4. `ParseProductUseCaseTest` 삭제 (또는 1건 스모크 축소)
5. `ParseProductUseCaseCachingTest` 업데이트 (ParserPipeline @Import)
6. 전체 테스트 통과
7. 빌드 통과

## 파일 변경

### 신규 (+2)
- `backend/src/main/kotlin/com/linkcart/application/parser/ParserPipeline.kt`
- `backend/src/test/kotlin/com/linkcart/application/parser/ParserPipelineTest.kt`

### 수정
- `backend/src/main/kotlin/com/linkcart/application/usecase/ParseProductUseCase.kt` — 슬림화
- `backend/src/test/kotlin/com/linkcart/application/usecase/ParseProductUseCaseCachingTest.kt` — @Import에 ParserPipeline 추가

### 삭제
- `backend/src/test/kotlin/com/linkcart/application/usecase/ParseProductUseCaseTest.kt` (테스트 케이스 ParserPipelineTest로 이관)

예상 diff: ~120줄 (주로 테스트 이동)

## 검증

```bash
cd backend && ./gradlew test
./gradlew build
```

- 기존 동작 변경 없음 (회귀 테스트로 보장)
- UseCase 슬림화 확인
