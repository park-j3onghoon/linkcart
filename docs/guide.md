# LinkCart 학습 가이드

> Kotlin + Spring Boot를 처음 접하는 개발자를 위한 가이드.
> Python/Django 경험이 있다면 대응 개념을 함께 설명합니다.

---

## 1. Kotlin 핵심 문법 (Python과 비교)

### 변수 선언

```kotlin
// Kotlin
val name: String = "iPhone 16"     // val = 불변 (Python의 상수 관례)
var price: Int = 1500000           // var = 가변
val currency = "KRW"              // 타입 추론 가능

// 널 안전성 — Kotlin의 가장 큰 특징
val discount: Int? = null          // ? 붙이면 null 가능
val amount: Int = discount ?: 0    // ?: = null이면 기본값 (Python의 or)
```

```python
# Python 대응
name: str = "iPhone 16"            # 타입 힌트 (런타임에 강제 안 됨)
price: int = 1500000
discount: int | None = None        # Optional
amount: int = discount or 0
```

**핵심 차이**: Kotlin은 null 체크가 **컴파일 타임**에 강제됨. `String`과 `String?`은 다른 타입.

### 함수

```kotlin
// Kotlin
fun calculateTotal(price: Int, quantity: Int = 1): Int {
    return price * quantity
}

// 한 줄이면 = 으로 축약
fun double(n: Int): Int = n * 2
```

```python
# Python 대응
def calculate_total(price: int, quantity: int = 1) -> int:
    return price * quantity
```

### data class (Python의 dataclass)

```kotlin
// Kotlin — equals, hashCode, toString, copy 자동 생성
data class Money(
    val amount: Int,
    val currency: String = "KRW",
)

val price = Money(15900)
val doubled = price.copy(amount = 31800)  // 불변 객체 복사
```

```python
# Python 대응
@dataclass(frozen=True)
class Money:
    amount: int
    currency: str = "KRW"
```

### sealed class (Python에 없는 개념)

```kotlin
// 가능한 하위 타입이 "봉인"됨 — 컴파일러가 모든 경우를 체크
sealed class ParseResult {
    data class Success(val product: Product) : ParseResult()
    data class Partial(val fields: Map<String, Any>) : ParseResult()
    data class Failure(val reason: String) : ParseResult()
}

// when 식 — 모든 경우를 처리하지 않으면 컴파일 에러!
fun handle(result: ParseResult): String = when (result) {
    is ParseResult.Success -> "상품: ${result.product.name}"
    is ParseResult.Partial -> "부분 파싱: ${result.fields.keys}"
    is ParseResult.Failure -> "실패: ${result.reason}"
}
```

이것은 Python의 `Union` 타입과 비슷하지만, **컴파일러가 모든 경우를 빠짐없이 처리하도록 강제**한다는 점이 핵심.

### enum class

```kotlin
enum class Mall(val displayName: String) {
    COUPANG("쿠팡"),
    ELEVENST("11번가"),
    GENERIC("기타"),
}

val mall = Mall.COUPANG
println(mall.displayName)  // "쿠팡"
```

### interface (Python의 Protocol/ABC)

```kotlin
// Kotlin
interface ProductParser {
    fun canParse(url: String): Boolean
    fun parse(url: String): ParseResult
}

// 구현
class OgParser : ProductParser {
    override fun canParse(url: String): Boolean = true
    override fun parse(url: String): ParseResult { /* ... */ }
}
```

```python
# Python 대응 (Protocol)
class ProductParser(Protocol):
    def can_parse(self, url: str) -> bool: ...
    def parse(self, url: str) -> ParseResult: ...
```

---

## 2. Spring Boot 핵심 개념

### Spring Boot란?

Django와 같은 웹 프레임워크. 차이점:

| 개념 | Django | Spring Boot |
|------|--------|-------------|
| 프로젝트 구조 | 강제 (settings, urls, views) | **자유** (패키지로 직접 구성) |
| DI (의존성 주입) | 없음 (직접 import) | **핵심 기능** (자동 주입) |
| ORM | Django ORM (내장) | JPA/Hibernate (선택) |
| 라우팅 | urls.py | **어노테이션** (@GetMapping 등) |
| 설정 | settings.py | application.yml |

### 어노테이션 (Annotation) = 코드에 붙이는 메타데이터

```kotlin
@RestController                      // "이 클래스는 API 컨트롤러입니다"
@RequestMapping("/api/v1/products")  // "URL 경로는 /api/v1/products"
class ProductController(
    private val parseUseCase: ParseProductUseCase,  // 자동 주입!
) {
    @PostMapping("/parse")           // "POST 요청을 받습니다"
    fun parse(@RequestBody request: ParseRequest): ParseResponse {
        // ...
    }
}
```

Django 대응:
```python
# Django에서는 urls.py에서 라우팅, views.py에서 로직
# Spring에서는 어노테이션으로 한 파일에서 해결
```

### 의존성 주입 (DI) — Spring의 핵심

**Django 방식** (직접 import):
```python
# views.py
from services.parser import OgParser

def parse_view(request):
    parser = OgParser()  # 직접 생성
    result = parser.parse(url)
```

**Spring 방식** (자동 주입):
```kotlin
// Controller는 인터페이스에만 의존
@RestController
class ProductController(
    private val parser: ProductParser,  // 인터페이스!
) {
    // Spring이 ProductParser 구현체를 자동으로 찾아서 넣어줌
}

// 구현체에 @Component를 붙이면 Spring이 관리
@Component
class OgParser : ProductParser { /* ... */ }
```

**왜 좋은가?**
- 테스트 시 mock 구현체를 쉽게 끼울 수 있음
- 구현체를 바꿔도 Controller 코드를 수정할 필요 없음
- 클린 아키텍처에서 **의존성 역전 원칙(DIP)**을 자연스럽게 구현

### 주요 어노테이션 정리

| 어노테이션 | 역할 | Django 대응 |
|-----------|------|-------------|
| `@SpringBootApplication` | 앱 진입점 | `manage.py` |
| `@RestController` | API 컨트롤러 | `views.py`의 view 함수 |
| `@Service` | 비즈니스 로직 서비스 | `services.py` |
| `@Component` | Spring이 관리하는 일반 컴포넌트 | 없음 (직접 import) |
| `@Configuration` | 설정 클래스 | `settings.py` |
| `@GetMapping` / `@PostMapping` | HTTP 메서드 라우팅 | `urls.py` + `@api_view` |
| `@RequestBody` | 요청 본문 파싱 | DRF의 `serializer.data` |
| `@Valid` | 요청 유효성 검증 | DRF의 `serializer.is_valid()` |
| `@Cacheable` | 결과 캐싱 | `@cache_page` / django-cache |

---

## 3. 클린 아키텍처 in Spring Boot

### 레이어 구조

```
┌─────────────────────────────────────────────┐
│  Presentation (api/, dto/)                  │  ← HTTP 요청/응답 처리
│  @RestController, Request/Response DTO      │
├─────────────────────────────────────────────┤
│  Application (port/, dto/, usecase/)        │  ← 비즈니스 유스케이스
│  @Service, interface(Port), sealed class    │
├─────────────────────────────────────────────┤
│  Infrastructure (client/, parser/, cache/)  │  ← 외부 시스템 연동
│  @Component, API 호출, HTML 파싱            │
├─────────────────────────────────────────────┤
│  Domain (entity/, vo/)                      │  ← 핵심 비즈니스 규칙
│  data class, enum class (어노테이션 없음!)   │
└─────────────────────────────────────────────┘
```

### 의존성 방향 (중요!)

```
Presentation → Application → Domain ← Infrastructure
                    ↑                      ↑
                    └──────────────────────┘
                    (Infrastructure가 Application의
                     interface를 구현)
```

- **Domain**: 어떤 프레임워크에도 의존하지 않음. 순수 Kotlin.
- **Application**: Domain에만 의존. `interface`(Port)를 정의.
- **Infrastructure**: Application의 interface를 구현. Spring, Jsoup 등 사용.
- **Presentation**: Application의 UseCase를 호출. Spring Web 사용.

### 각 레이어의 파일과 역할

#### Domain — `com.linkcart.domain`
```
domain/
├── entity/
│   └── Product.kt          ← 상품 엔티티 (핵심 비즈니스 객체)
└── vo/
    ├── Money.kt             ← 가격 (값 객체 — 불변, 동등성으로 비교)
    └── Mall.kt              ← 쇼핑몰 종류 (enum)
```

**규칙**: Spring 어노테이션 없음. import도 Kotlin 표준 라이브러리만. 테스트가 가장 쉬운 레이어.

#### Application — `com.linkcart.application`
```
application/
├── port/
│   ├── ProductParser.kt     ← 파서 인터페이스 (구현은 Infrastructure에서)
│   └── ProductRepository.kt ← 저장소 인터페이스 (Phase 2용)
├── dto/
│   └── ParseResult.kt       ← 파싱 결과 (sealed class)
└── usecase/
    └── ParseProductUseCase.kt ← "URL을 파싱하라" 유스케이스
```

**규칙**: Domain에만 의존. `interface`로 외부 의존성 추상화. `@Service` 어노테이션 사용.

```kotlin
// UseCase 예시 — 핵심 비즈니스 로직
@Service
class ParseProductUseCase(
    private val parserFactory: ParserFactory,  // 인터페이스 주입
) {
    fun execute(url: String): ParseResult {
        val parser = parserFactory.getParser(url)
        return parser.parse(url)
    }
}
```

#### Infrastructure — `com.linkcart.infrastructure`
```
infrastructure/
├── client/
│   ├── CoupangApiClient.kt   ← 쿠팡 어필리에이트 API 호출
│   └── ElevenStApiClient.kt  ← 11번가 API 호출
├── parser/
│   ├── OgParser.kt            ← Jsoup으로 OG 태그 파싱
│   └── ParserFactory.kt       ← URL → 적절한 파서 선택
└── cache/
    └── (Spring Cache + Caffeine으로 자동 관리)
```

**규칙**: 외부 라이브러리(Jsoup, HTTP 클라이언트) 사용. Application의 interface를 `@Component`로 구현.

```kotlin
// OgParser — Application의 ProductParser interface를 구현
@Component
class OgParser : ProductParser {
    override fun canParse(url: String): Boolean = true  // 모든 URL 가능 (폴백)

    override fun parse(url: String): ParseResult {
        val doc = Jsoup.connect(url).get()          // Jsoup으로 HTML 가져오기
        val title = doc.select("meta[property=og:title]").attr("content")
        // ...
    }
}
```

#### Presentation — `com.linkcart.presentation`
```
presentation/
├── api/
│   ├── ProductController.kt      ← 상품 파싱 API
│   ├── ImageProxyController.kt   ← 이미지 프록시
│   └── HealthController.kt       ← 헬스 체크
└── dto/
    ├── ParseRequest.kt            ← 요청 DTO (JSON → Kotlin)
    └── ParseResponse.kt           ← 응답 DTO (Kotlin → JSON)
```

**규칙**: HTTP 관련 로직만. 비즈니스 로직 없음. UseCase를 호출하고 결과를 변환.

```kotlin
@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val parseProductUseCase: ParseProductUseCase,
) {
    @PostMapping("/parse")
    fun parse(@Valid @RequestBody request: ParseRequest): ResponseEntity<ParseResponse> {
        val result = parseProductUseCase.execute(request.url)
        return when (result) {
            is ParseResult.Success -> ResponseEntity.ok(ParseResponse.from(result))
            is ParseResult.Partial -> ResponseEntity.ok(ParseResponse.fromPartial(result))
            is ParseResult.Failure -> ResponseEntity.unprocessableEntity()
                .body(ParseResponse.fromFailure(result))
        }
    }
}
```

---

## 4. 데이터 흐름 (요청 → 응답)

```
[사용자] → POST /api/v1/products/parse { "url": "https://coupang.com/..." }
    │
    ▼
[ProductController]  ← JSON을 ParseRequest로 변환
    │
    ▼
[ParseProductUseCase]  ← 비즈니스 로직: 어떤 파서를 쓸지 결정
    │
    ▼
[ParserFactory.getParser(url)]  ← URL 도메인 보고 파서 선택
    │
    ├── coupang.com → [CoupangApiClient] → 어필리에이트 API 호출
    ├── 11st.co.kr  → [ElevenStApiClient] → 어필리에이트 API 호출
    └── 기타         → [OgParser] → Jsoup으로 OG 태그 파싱
    │
    ▼
[ParseResult] (Success / Partial / Failure)
    │
    ▼
[ProductController]  ← ParseResult를 ParseResponse로 변환
    │
    ▼
[사용자] ← JSON 응답 { "name": "...", "price": {...}, ... }
```

---

## 5. 빌드 & 실행

### Gradle = Python의 pip + Makefile

```bash
./gradlew bootRun        # 서버 실행 (Django의 python manage.py runserver)
./gradlew build          # 빌드 + 테스트 (pip install + pytest)
./gradlew test           # 테스트만 (pytest)
./gradlew clean          # 빌드 결과 삭제
```

### 프로젝트 실행 순서

```bash
# 1. 백엔드
cd backend
./gradlew bootRun
# → http://localhost:8080/health 에서 확인
# → http://localhost:8080/docs 에서 Swagger UI

# 2. 웹 프론트
cd apps/web
npm run dev
# → http://localhost:3000

# 3. 모바일
cd apps/mobile
npx expo start
```

---

## 6. 테스트 (JUnit 5 + Kotlin)

```kotlin
// pytest 스타일과 비교

// Python (pytest)
def test_money_creation():
    money = Money(amount=15900, currency="KRW")
    assert money.amount == 15900

// Kotlin (JUnit 5)
class MoneyTest {
    @Test
    fun `Money 생성 시 금액과 통화가 설정된다`() {
        val money = Money(amount = 15900, currency = "KRW")
        assertEquals(15900, money.amount)
        assertEquals("KRW", money.currency)
    }
}
```

**Kotlin 테스트 특징**:
- 함수 이름에 백틱(\`)을 쓰면 한글/공백 가능
- `@Test` 어노테이션으로 테스트 메서드 표시
- `assertEquals`, `assertTrue`, `assertThrows` 등 사용
- Spring 통합 테스트는 `@SpringBootTest` 어노테이션 추가

---

## 7. 용어 매핑 (Python → Kotlin/Spring)

| Python/Django | Kotlin/Spring Boot | 설명 |
|---------------|-------------------|------|
| `dataclass` | `data class` | 불변 데이터 객체 |
| `Protocol` / `ABC` | `interface` | 추상 타입 정의 |
| `Union[A, B]` | `sealed class` | 제한된 하위 타입 |
| `Enum` | `enum class` | 열거형 |
| `dict` | `Map<K, V>` | 딕셔너리 |
| `list` | `List<T>` | 리스트 (불변) / `MutableList<T>` (가변) |
| `None` | `null` | 값 없음 |
| `Optional[T]` | `T?` | null 가능 타입 |
| `raise Exception` | `throw Exception` | 예외 발생 |
| `try/except` | `try/catch` | 예외 처리 |
| `pip install` | `gradle build` | 의존성 설치 + 빌드 |
| `pytest` | `./gradlew test` | 테스트 실행 |
| `views.py` | `@RestController` | API 엔드포인트 |
| `serializer` | Request/Response DTO | 요청/응답 직렬화 |
| `settings.py` | `application.yml` | 설정 파일 |
| `urls.py` | `@RequestMapping` | URL 라우팅 |
| DRF `@api_view` | `@PostMapping` | HTTP 메서드 바인딩 |
| `@cache_page` | `@Cacheable` | 캐싱 |
