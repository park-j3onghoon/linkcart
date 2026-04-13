# Linkcart Kotlin/Spring 학습 시험지

이 시험지는 `docs/guide.html`, `CLAUDE.md`, 현재 `backend/` 코드 기준으로 만들었다.
목표는 "문법을 외웠는지"보다 "Linkcart 코드를 읽고 다음 작업을 시작할 준비가 되었는지"를 확인하는 것이다.

## 응시 방식

- 권장 시간: 60~90분
- 1회차는 가능하면 가이드 문서를 닫고 풀기
- 막히는 문제는 빈칸으로 두지 말고, 지금 이해한 수준대로 적기
- 답안은 채팅으로 보내면 내가 채점하고, 취약한 개념만 다시 연결해서 설명해준다

## 점수 기준

- 85점 이상: 입문 구간은 통과. 작은 기능 추가를 시작해도 된다.
- 70~84점: 개념은 잡혔지만, 코드 작성 전에 몇 군데 보강이 필요하다.
- 50~69점: 가이드는 읽었지만 연결이 약하다. 다시 보면 효율이 좋다.
- 49점 이하: 아직은 용어가 분리되어 있다. 시험보다 예제 따라치기가 먼저다.

## 답안 형식

아래 형식으로 번호만 맞춰서 보내면 된다.

```md
1. B
2. C
3. A
...
11. Spring Boot의 Controller는 ...
...
22.
```kotlin
data class ...
```
```

---

## A. 객관식 (각 2점, 총 20점)

### 1.
JDK가 이 프로젝트에서 필요한 가장 직접적인 이유는 무엇인가?

A. TypeScript를 실행하기 위해  
B. Kotlin/Java 코드를 컴파일하고 실행하기 위해  
C. Docker 이미지를 빌드하기 위해  
D. Swagger UI를 보기 위해

### 2.
Linkcart 백엔드는 현재 Java 21을 사용한다. 가장 적절한 이유는 무엇인가?

A. Java 21만 Kotlin을 지원해서  
B. Java 25는 유료라서  
C. Java 21은 LTS이고 현재 Gradle/Spring 생태계와 안정적으로 맞기 때문에  
D. Spring Boot 3.5가 Java 17 이상을 금지해서

### 3.
Kotlin에서 `val`과 `var`의 차이로 맞는 것은?

A. `val`은 nullable이고 `var`는 non-null이다  
B. `val`은 읽기 전용 참조, `var`는 재할당 가능 참조다  
C. `val`은 함수 안에서만 쓰고 `var`는 클래스에서만 쓴다  
D. `val`은 String 전용, `var`는 숫자 전용이다

### 4.
`data class Product(...)`를 쓴 이유로 가장 적절한 것은?

A. Spring Bean으로 자동 등록되기 때문에  
B. JSON 직렬화가 data class에서만 가능하기 때문에  
C. 값 중심 객체에 유용한 `equals`, `hashCode`, `toString`, `copy` 등을 자동으로 얻기 위해  
D. enum을 상속하려면 data class여야 해서

### 5.
`Mall`을 `enum class`로 둔 주된 이유는 무엇인가?

A. 값의 후보를 제한하고 오타를 줄이기 위해  
B. 네트워크 요청 속도를 높이기 위해  
C. DB 테이블을 자동 생성하기 위해  
D. null을 금지하기 위해

### 6.
`ParseResult`를 `sealed interface`로 만든 장점으로 가장 적절한 것은?

A. 클래스 수를 줄일 수 있다  
B. 런타임에만 타입을 확인할 수 있다  
C. `when`에서 가능한 결과 타입을 컴파일 시점에 안전하게 다루기 좋다  
D. 인터페이스는 테스트가 불가능해서

### 7.
`build.gradle.kts`의 `repositories { mavenCentral() }`는 무엇을 의미하는가?

A. API 엔드포인트 목록  
B. 의존성 라이브러리를 받아올 저장소 위치  
C. 애플리케이션 설정 파일 경로  
D. 테스트 결과 저장 디렉토리

### 8.
`@RestController`의 역할로 맞는 것은?

A. 클래스가 HTTP 요청을 받아 응답을 반환하는 웹 컨트롤러임을 나타낸다  
B. 클래스를 DB 엔티티로 등록한다  
C. 클래스를 테스트 전용으로 만든다  
D. 캐시를 자동으로 활성화한다

### 9.
`application.yml`의 `property-naming-strategy: SNAKE_CASE` 설정 효과로 가장 적절한 것은?

A. Kotlin 변수명이 모두 snake_case로 강제된다  
B. JSON 응답 필드가 `imageUrl` 대신 `image_url`처럼 내려가게 된다  
C. DB 컬럼명이 자동 생성된다  
D. YAML 키만 snake_case로 써야 한다

### 10.
`WebConfig`에서 CORS를 설정하는 이유는 무엇인가?

A. 백엔드가 프론트엔드의 다른 Origin 요청을 허용하기 위해  
B. Kotlin 컴파일 속도를 높이기 위해  
C. 캐시 만료 시간을 정하기 위해  
D. OpenAPI 스키마를 생성하기 위해

---

## B. 단답형 (각 5점, 총 30점)

### 11.
Django의 `urls.py + view` 조합과 Spring Boot의 `Controller + @GetMapping/@PostMapping` 관계를 2~4문장으로 비교해 설명하라.

### 12.
`Money.amount`를 `Double` 대신 `Long`으로 두는 이유를 설명하라.

### 13.
현재 `ProductRepository`는 인터페이스만 있고 구현체가 없다. 이렇게 시작하는 이유를 Linkcart 맥락에서 설명하라.

### 14.
`Product`는 Domain에 있고 `ParseResult`는 Application에 있다. 왜 같은 위치에 두지 않는가?

### 15.
`parserUsed`와 `fallbackUsed`가 왜 같이 필요한지 설명하라.

### 16.
`/docs`와 `/openapi.json`이 프론트엔드와 협업할 때 왜 유용한지 설명하라.

---

## C. 코드 읽기 (각 5점, 총 25점)

아래 문제는 실제 코드 기준이다.

### 17.
파일: `backend/src/main/kotlin/com/linkcart/LinkcartBackendApplication.kt`

```kotlin
@SpringBootApplication
class LinkcartBackendApplication

fun main(args: Array<String>) {
    runApplication<LinkcartBackendApplication>(*args)
}
```

`main` 함수와 `runApplication<...>()`가 하는 일을 설명하라.

### 18.
파일: `backend/src/main/kotlin/com/linkcart/presentation/api/HealthController.kt`

```kotlin
@RestController
class HealthController {

    @GetMapping("/health")
    fun health(): Map<String, String> = mapOf("status" to "ok")
}
```

브라우저나 프론트엔드가 `GET /health`를 호출하면 어떤 JSON이 내려오는지 적고, 이 엔드포인트가 왜 먼저 있는 게 좋은지 설명하라.

### 19.
파일: `backend/src/main/kotlin/com/linkcart/infrastructure/config/WebConfig.kt`

```kotlin
registry.addMapping("/**")
    .allowedOrigins("http://localhost:3000")
    .allowedMethods("GET", "POST", "OPTIONS")
    .allowedHeaders("*")
    .allowCredentials(true)
```

현재 허용되는 Origin과 메서드는 무엇인가? 그리고 모바일 앱이나 배포 환경으로 갈 때 어떤 점을 다시 봐야 하는가?

### 20.
파일: `backend/src/main/kotlin/com/linkcart/application/dto/ParseResult.kt`

```kotlin
sealed interface ParseResult {
    data class Success(
        val product: Product,
        val parserUsed: String,
        val fallbackUsed: Boolean = false,
    ) : ParseResult

    data class Partial(
        val fields: Map<String, Any>,
        val parserUsed: String,
        val fallbackUsed: Boolean = false,
    ) : ParseResult

    data class Failure(
        val reason: String,
        val parserUsed: String,
    ) : ParseResult
}
```

`Success`, `Partial`, `Failure`를 각각 어떤 상황에서 쓸지 Linkcart의 상품 링크 파싱 흐름에 맞춰 설명하라.

### 21.
파일: `backend/src/main/kotlin/com/linkcart/domain/vo/Mall.kt`

```kotlin
enum class Mall(val displayName: String) {
    COUPANG("쿠팡"),
    ELEVENST("11번가"),
    GENERIC("기타"),
}
```

왜 `"coupang"`, `"11st"`, `"generic"` 같은 raw string 대신 enum을 쓰는 것이 더 나은지 설명하라.

---

## D. 작성형 / 실습형 (총 25점)

### 22. Kotlin DTO 작성 (8점)
아래 조건을 만족하는 Kotlin 코드를 작성하라.

- 이름: `ParseProductRequest`
- 필드: `url: String`
- 값 보관용 객체이므로 가장 적절한 Kotlin 문법을 사용할 것

### 23. 간단한 Controller 작성 (8점)
아래 요구사항을 만족하는 Spring Boot 코드를 작성하라.

- `GET /hello`
- JSON 응답: `{ "message": "hello" }`
- 클래스와 메서드 전체를 적을 것

### 24. `when`으로 결과 처리 (9점)
아래 요구사항을 만족하는 Kotlin 함수를 작성하라.

- 함수명: `toMessage`
- 입력: `result: ParseResult`
- 반환: `String`
- `Success`면 `"성공: {parserUsed}"`
- `Partial`면 `"부분 성공: {parserUsed}"`
- `Failure`면 `"실패: {reason}"`

---

## E. 보너스 (선택, 10점)

### 25.
Linkcart에서 앞으로 `OgParser`, `CoupangApiClient`, `ElevenStApiClient`, `ProductParseUseCase` 같은 코드가 추가된다고 가정하자.
각 요소를 `Domain / Application / Infrastructure / Presentation` 중 어디에 둘지 나누고, 이유를 4~6문장으로 설명하라.

---

## 제출 가이드

- 답안은 이 파일을 복사하지 말고 채팅에 번호별로 보내면 된다.
- 전부 한 번에 보내도 되고, `1~10`, `11~18`처럼 나눠 보내도 된다.
- 내가 채점할 때는
  - 맞고 틀림
  - 왜 틀렸는지
  - 다시 볼 가이드 섹션
  - 다음에 바로 해볼 구현 과제
  를 같이 정리해준다.
