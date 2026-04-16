# Phase 1 상세 작업 계획 (PR 2 ~ PR 7)

> 이 문서는 PR 1(프로젝트 초기 세팅) 완료 이후의 상세 구현 계획이다.
> 각 PR은 100~200줄 규모, TDD 방식으로 진행한다.

---

## PR 2: 도메인 포트 이동 + OG 파서 구현 (TDD)

### PR 제목
`OG 태그 파서 구현 및 도메인 포트 구조 정리`

### 목적
- `application/port/`에 있는 `ProductParser`, `ProductRepository` 인터페이스를 `domain/port/`로 이동하여 클린 아키텍처 원칙(포트는 도메인 계약)에 맞춘다.
- 모든 URL에 대한 폴백 파서인 OgParser를 구현하여, 이후 PR에서 어필리에이트 API가 실패했을 때 대비할 수 있는 기반을 만든다.
- ParserFactory의 기본 구조를 잡아 URL 도메인 기반 파서 라우팅의 뼈대를 세운다.

### 작업 항목

| # | 파일 | 내용 |
|---|------|------|
| 1 | `domain/port/ProductParser.kt` | `application/port/`에서 이동. 패키지 선언만 변경. |
| 2 | `domain/port/ProductRepository.kt` | `application/port/`에서 이동. 패키지 선언만 변경. |
| 3 | `application/port/` | 디렉토리 삭제 (이동 완료 후). 기존 import 경로 일괄 수정. |
| 4 | `infrastructure/adapter/parser/OgParser.kt` | `@Component`. Jsoup으로 OG meta 태그(`og:title`, `og:image`, 가격 추정) 파싱. `ProductParser` 구현. `canParse()`는 항상 `true` (범용 폴백). |
| 5 | `infrastructure/adapter/parser/ParserFactory.kt` | `@Component`. URL 도메인 분석하여 적절한 파서 반환. 현재는 OgParser만 등록. `getParser(url): ProductParser` 메서드. |
| 6 | `test/.../infrastructure/adapter/parser/OgParserTest.kt` | 4개 케이스: (1) 정상 OG 태그 파싱 (2) 부분 OG (title만 있고 image 없음) (3) OG 태그 없는 HTML (4) 비HTML 응답 (PDF 등). Jsoup 대신 HTML 문자열 직접 주입. |
| 7 | `test/.../infrastructure/adapter/parser/ParserFactoryTest.kt` | URL 도메인별 올바른 파서 반환 검증. |

### TDD 순서

```
1. OgParserTest 4개 케이스 작성 (RED)
   - 정상 OG → ParseResult.Success 검증
   - 부분 OG → ParseResult.Partial 검증
   - OG 없음 → ParseResult.Partial (title 태그 폴백)
   - 비HTML → ParseResult.Failure 검증

2. OgParser 구현 (GREEN)
   - Jsoup으로 HTML 파싱
   - og:title, og:image, og:price:amount 등 추출
   - 결과에 따라 Success/Partial/Failure 반환

3. ParserFactoryTest 작성 (RED)
   - 임의 URL → OgParser 반환 검증
   - coupang.com URL → (PR 3에서 추가 예정, 현재는 OgParser)

4. ParserFactory 구현 (GREEN)

5. 포트 이동 + import 수정 (REFACTOR)
```

### 검증 방법
```bash
cd backend && ./gradlew test
# OgParserTest 4개 PASS
# ParserFactoryTest PASS
# 기존 contextLoads PASS (포트 이동 후 빌드 정상 확인)
```

### 학습 문서
`docs/pr/pr2.html` 작성 -- 아래 개념 정리:
- **Jsoup 라이브러리**: HTML 파싱 기본 사용법, `select()`, `attr()`, OG meta 태그 구조
- **@Component와 의존성 주입**: Spring이 OgParser를 자동 탐지하고 주입하는 흐름
- **interface 구현 패턴**: `ProductParser` interface를 OgParser가 구현하는 구조, Django Protocol과 비교
- **Factory 패턴**: ParserFactory가 URL 기반으로 파서를 선택하는 전략 패턴
- **TDD Red-Green-Refactor**: 테스트 먼저 작성하는 실전 흐름
- **패키지 이동과 import**: Kotlin/Gradle에서 패키지 구조 변경 시 주의점

### 티니어 면접 매핑
| 학습 내용 | 티니어 요구사항 |
|-----------|----------------|
| Jsoup HTML 파싱 | 외부 데이터 수집 역량 (유틸리티 앱에서 외부 소스 파싱 필수) |
| Factory 패턴 | 확장 가능한 아키텍처 설계 (파서 추가 시 OCP 준수) |
| interface + DI | Spring 기반 서버 개발의 핵심 패턴 |
| TDD | 안정적인 코드 품질 보장 (장애 없는 환경 유지) |

---

## PR 3: 어필리에이트 API 클라이언트 + Fallback Chain (TDD)

### PR 제목
`쿠팡 Open API/11번가 Open API 클라이언트 및 폴백 체인 구현`

### 목적
- 쿠팡 Seller Open API(HMAC), 11번가 Open API 클라이언트를 구현하여 전용 쇼핑몰 파싱을 지원한다.
- ParserFactory에 쿠팡/11번가 파서를 등록하고 URL 도메인 기반 라우팅을 완성한다.
- ParseProductUseCase에서 전용 파서 실패 시 OgParser로 폴백하는 체인을 구현한다.

### 작업 항목

| # | 파일 | 내용 |
|---|------|------|
| 1 | `infrastructure/adapter/parser/CoupangApiClient.kt` | `@Component`, `ProductParser` 구현. 쿠팡 Seller Open API(HMAC) 호출. `canParse(url)`: coupang.com 도메인 매칭. access key/secret key는 `application.yml`에서 `@Value`로 주입. |
| 2 | `infrastructure/adapter/parser/ElevenStApiClient.kt` | `@Component`, `ProductParser` 구현. 11번가 Open API 호출. `canParse(url)`: 11st.co.kr 도메인 매칭. |
| 3 | `application/parser/ParserFactory.kt` | 수정: 쿠팡/11번가 파서 등록. `getParser(url)` + `getFallback(): OgParser` 추가. |
| 4 | `application/usecase/ParseProductUseCase.kt` | `@Service`. ParserFactory에서 파서 선택 → 파싱 시도 → 실패 시 OgParser 폴백. `@Cacheable`로 TTL 캐시 적용. |
| 5 | `application.yml` | 외부 API 설정 placeholder 추가 (`linkcart.coupang.access-key`, `linkcart.coupang.secret-key`, `linkcart.elevenst.api-key`). |
| 6 | `test/.../CoupangApiClientTest.kt` | Mock API 응답으로 단위 테스트: 정상 응답, API 실패(4xx/5xx), 타임아웃, 상품 삭제됨, 품절 상품. |
| 7 | `test/.../ElevenStApiClientTest.kt` | Mock API 응답으로 단위 테스트: 정상 응답, API 실패, 타임아웃, 상품 없음. |
| 8 | `test/.../ParseProductUseCaseTest.kt` | Fallback chain 통합 테스트 5개 시나리오: (1) 전용 파서 성공 (2) 전용 실패 → OG 폴백 성공 (3) 전용 실패 → OG 부분 성공 (4) 전용 + OG 모두 실패 (5) 타임아웃 처리. |

### TDD 순서

```
1. CoupangApiClientTest 작성 (RED)
   - 정상 API 응답 → ParseResult.Success
   - 4xx 에러 → ParseResult.Failure
   - 타임아웃 → ParseResult.Failure
   - 품절/삭제 → ParseResult.Failure (각각 다른 reason)

2. CoupangApiClient 구현 (GREEN)
   - RestTemplate/WebClient로 API 호출
   - 응답 JSON → Product 매핑
   - 에러/타임아웃 처리

3. ElevenStApiClientTest 작성 (RED)
4. ElevenStApiClient 구현 (GREEN)

5. ParseProductUseCaseTest 5개 시나리오 작성 (RED)
   - 파서는 mock으로 주입 (interface 기반이므로 가능)

6. ParseProductUseCase 구현 (GREEN)
   - ParserFactory.getParser(url) → parse() 시도
   - Failure 시 → ParserFactory.getFallback() → parse() 재시도
   - @Cacheable("products") 적용

7. ParserFactory 수정 (REFACTOR)
   - 쿠팡/11번가 파서 등록
```

### 검증 방법
```bash
cd backend && ./gradlew test
# CoupangApiClientTest 5개 PASS
# ElevenStApiClientTest 4개 PASS
# ParseProductUseCaseTest 5개 PASS
# 기존 OgParserTest, ParserFactoryTest PASS
```

### 학습 문서
`docs/pr/pr3.html` 작성 -- 아래 개념 정리:
- **RestTemplate vs WebClient**: Spring에서 HTTP 클라이언트 선택 기준, 동기/비동기 차이
- **@Value와 외부 설정**: access key/secret key를 코드에 하드코딩하지 않고 application.yml에서 주입하는 패턴
- **@Cacheable**: Spring Cache 추상화, Caffeine TTL 캐시와의 연동, 캐시 키 전략
- **Fallback Chain 패턴**: 전용 파서 → 범용 폴백의 전략, 장애 격리 관점
- **Mock 테스트**: interface 기반 의존성을 mock으로 교체하여 단위 테스트하는 방법
- **전용 API 구조**: 쿠팡 Seller Open API(HMAC) / 11번가 Open API의 인증 방식과 응답 구조

### 티니어 면접 매핑
| 학습 내용 | 티니어 요구사항 |
|-----------|----------------|
| 외부 API 클라이언트 | 외부 API 연동 + 장애 격리 (24/365 안정성) |
| Fallback chain | 장애 대응 전략 (외부 의존성 실패 시 graceful degradation) |
| @Cacheable + Caffeine | 캐시 전략 (대규모 트래픽 성능 최적화) |
| Mock 테스트 | 테스트 가능한 설계 (코드 리뷰, 품질) |
| @Value 외부 설정 | 환경별 설정 분리 (운영 환경 안정화) |

---

## PR 4: API 엔드포인트 + 이미지 프록시 (TDD)

### PR 제목
`상품 파싱 API 엔드포인트 및 이미지 프록시 구현`

### 목적
- 프론트엔드가 호출할 `POST /api/v1/products/parse` 엔드포인트를 구현한다.
- OG 파서 이미지의 CORS/핫링크 문제를 해결하기 위한 이미지 프록시 엔드포인트를 구현한다.
- Presentation 레이어의 요청/응답 DTO를 정의하고, URL 유효성 검증을 레이어별로 분리한다.
- 이 PR 완료 후 백엔드 API가 완성되어, 프론트엔드 개발을 시작할 수 있다.

### 작업 항목

| # | 파일 | 내용 |
|---|------|------|
| 1 | `presentation/dto/ParseRequest.kt` | `data class ParseRequest(@field:NotBlank val url: String)`. Jakarta Validation으로 URL 형식 검증. |
| 2 | `presentation/dto/ParseResponse.kt` | `data class ParseResponse(name, price, imageUrl, sourceUrl, mall, partial, parserUsed, fallbackUsed)`. ParseResult → ParseResponse 변환 companion 함수 포함. |
| 3 | `presentation/dto/ErrorResponse.kt` | `data class ErrorResponse(val code: String, val message: String)`. 통일된 에러 응답 형식. |
| 4 | `presentation/api/ProductController.kt` | `@RestController @RequestMapping("/api/v1/products")`. `@PostMapping("/parse")` + `@Valid @RequestBody`. ParseProductUseCase 호출 → ParseResponse 반환. |
| 5 | `presentation/api/ImageProxyController.kt` | `@RestController`. `@GetMapping("/api/v1/images/proxy")`. URL 파라미터로 받은 이미지를 fetch하여 바이트 스트림으로 전달. Content-Type 헤더 전달. |
| 6 | `presentation/api/GlobalExceptionHandler.kt` | `@RestControllerAdvice`. Validation 에러, 파싱 실패 등 통일된 에러 응답 처리. |
| 7 | `test/.../presentation/api/ProductControllerTest.kt` | MockMvc 테스트: 정상 파싱, 잘못된 URL, 빈 URL, 파싱 실패. UseCase는 mock. |
| 8 | `test/.../presentation/api/ImageProxyControllerTest.kt` | MockMvc 테스트: 정상 이미지 프록시, 잘못된 URL, 이미지 fetch 실패. |

### TDD 순서

```
1. ProductControllerTest 작성 (RED)
   - POST /api/v1/products/parse + 정상 URL → 200 + ParseResponse JSON
   - POST + 빈 URL → 400 + ErrorResponse
   - POST + 잘못된 URL 형식 → 400 + ErrorResponse
   - UseCase가 Failure 반환 → 502 + ErrorResponse

2. ParseRequest, ParseResponse, ErrorResponse 작성
3. ProductController 구현 (GREEN)
4. GlobalExceptionHandler 구현

5. ImageProxyControllerTest 작성 (RED)
   - GET /api/v1/images/proxy?url=https://... → 200 + image bytes
   - GET + 빈 url → 400
   - 이미지 fetch 실패 → 502

6. ImageProxyController 구현 (GREEN)

7. 응답 JSON snake_case 확인 (REFACTOR)
   - application.yml의 SNAKE_CASE 설정이 ParseResponse에 적용되는지 검증
```

### 검증 방법
```bash
cd backend && ./gradlew test
# ProductControllerTest 4개 PASS
# ImageProxyControllerTest 3개 PASS
# 기존 모든 테스트 PASS

# 수동 검증: 서버 띄우고 Swagger UI 확인
./gradlew bootRun
# 브라우저에서 http://localhost:8080/docs 열어 API 스키마 확인
# curl로 실제 호출 테스트
curl -X POST http://localhost:8080/api/v1/products/parse \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.example.com"}'
```

### 학습 문서
`docs/pr/pr4.html` 작성 -- 아래 개념 정리:
- **@RestController + @RequestMapping**: 엔드포인트 매핑 방법, Django urls.py + views.py 비교
- **@Valid + Jakarta Validation**: Bean Validation으로 요청 검증, Django serializer.is_valid() 비교
- **@RequestBody, @RequestParam**: JSON body와 query parameter 바인딩 차이
- **@RestControllerAdvice**: 전역 예외 처리, Django middleware exception handler 비교
- **MockMvc 테스트**: Spring MVC 테스트 도구, HTTP 요청을 코드로 시뮬레이션하는 방법
- **이미지 프록시 패턴**: 외부 이미지의 CORS 문제 해결, byte stream 전달
- **OpenAPI/Swagger**: springdoc이 자동 생성하는 API 문서, 프론트엔드 협업에 활용

### 티니어 면접 매핑
| 학습 내용 | 티니어 요구사항 |
|-----------|----------------|
| REST API 설계 | API 설계 역량 (글로벌 멀티 플랫폼 지원) |
| Bean Validation | 입력 검증 + 보안 (안정적 운영) |
| GlobalExceptionHandler | 통일된 에러 처리 (클라이언트 친화적 API) |
| MockMvc | 통합 테스트 역량 (코드 품질 보장) |
| OpenAPI 자동 문서화 | 프론트엔드/모바일 협업 (타입 동기화) |
| 이미지 프록시 | 외부 리소스 안전한 전달 (보안, UX) |

---

## PR 5: Shared 패키지 타입 동기화 + Web UI 구현

### PR 제목
`OpenAPI 타입 동기화 및 Next.js 웹 UI 구현`

### 목적
- PR 4에서 완성된 백엔드 API의 OpenAPI 스키마로부터 TypeScript 타입을 자동 생성하여 타입 안전성을 확보한다.
- Next.js로 사용자가 URL을 입력하고 상품 정보를 확인하는 핵심 UI를 구현한다.
- localStorage로 상품 목록을 저장/복원하여 브라우저 새로고침 후에도 데이터가 유지되도록 한다.

### 작업 항목

| # | 파일 | 내용 |
|---|------|------|
| 1 | `packages/shared/src/types/product.ts` | OpenAPI codegen으로 자동 생성된 타입으로 교체. 수동 정의 주석 제거. |
| 2 | `packages/shared/package.json` | `codegen` 스크립트: `npx openapi-typescript http://localhost:8080/openapi.json -o src/types/generated.ts` |
| 3 | `apps/web/src/hooks/useProducts.ts` | 커스텀 훅: 상품 목록 상태 관리, 파싱 요청, 중복 URL 체크, localStorage 저장/복원. |
| 4 | `apps/web/src/hooks/useLocalStorage.ts` | 커스텀 훅: localStorage 읽기/쓰기 + 손상 데이터 graceful 처리 (JSON.parse 실패 시 초기값). |
| 5 | `apps/web/src/components/UrlInput.tsx` | URL 입력 컴포넌트: 텍스트 입력 + "추가" 버튼 + 유효성 에러 메시지 + 중복 URL 경고. shared의 `validateUrl` 사용. |
| 6 | `apps/web/src/components/ProductCard.tsx` | 상품 카드 컴포넌트: 이미지 + 상품명 + 가격 + 쇼핑몰 뱃지. 이미지 로드 실패 시 플레이스홀더. |
| 7 | `apps/web/src/components/ProductList.tsx` | 상품 리스트 컴포넌트: ProductCard 그리드. 빈 상태 안내 UI. |
| 8 | `apps/web/src/components/ParseProgress.tsx` | 파싱 진행 표시 컴포넌트: "URL 확인 중..." → "상품 정보 파싱 중..." → "완료". |
| 9 | `apps/web/src/app/page.tsx` | 메인 페이지: UrlInput + ParseProgress + ProductList 조합. useProducts 훅 연결. |
| 10 | `apps/web/src/app/layout.tsx` | 레이아웃 수정: 한글 lang, LinkCart 제목, 기본 메타데이터. |

### TDD 순서

```
1. useLocalStorage 훅 테스트 작성 (RED)
   - 초기값 반환
   - 값 저장 후 재호출 시 저장값 반환
   - 손상 데이터 시 초기값 폴백

2. useLocalStorage 구현 (GREEN)

3. UrlInput 컴포넌트 테스트 작성 (RED)
   - 빈 입력 제출 시 에러 메시지
   - 잘못된 URL 제출 시 에러 메시지
   - 정상 URL 제출 시 onSubmit 콜백 호출

4. UrlInput 구현 (GREEN)

5. ProductCard 컴포넌트 테스트 작성 (RED)
   - 상품 정보 정상 렌더링
   - 이미지 로드 실패 시 플레이스홀더 표시

6. ProductCard 구현 (GREEN)

7. ProductList + ParseProgress 구현
8. page.tsx 조합 (REFACTOR)
```

### 검증 방법
```bash
# 타입 동기화 (백엔드 서버 실행 필요)
cd packages/shared && npm run codegen

# 웹 테스트
cd apps/web && npm run test

# 웹 빌드 확인
cd apps/web && npm run build

# 수동 검증
cd apps/web && npm run dev
# http://localhost:3000 에서:
# 1. URL 입력 → 파싱 → 카드 표시
# 2. 같은 URL 재입력 → 중복 경고
# 3. 브라우저 새로고침 → 데이터 복원
# 4. 잘못된 URL → 에러 메시지
```

### 학습 문서
`docs/pr/pr5.html` 작성 -- 아래 개념 정리:
- **OpenAPI codegen**: 백엔드 스키마 → TypeScript 타입 자동 생성 파이프라인
- **Next.js App Router**: `app/` 디렉토리 구조, layout.tsx와 page.tsx의 역할, Django의 URLconf + template 비교
- **React 커스텀 훅**: `useProducts`, `useLocalStorage` 패턴, 상태 관리와 부수효과 분리
- **컴포넌트 합성**: UrlInput + ProductCard + ProductList를 page.tsx에서 조합하는 패턴
- **localStorage 안전 사용**: 시크릿 모드, 스토리지 꽉 참, JSON 파싱 실패 등 엣지 케이스
- **Vitest + Testing Library**: React 컴포넌트 테스트 방법, render/screen/fireEvent 사용법

### 티니어 면접 매핑
| 학습 내용 | 티니어 요구사항 |
|-----------|----------------|
| OpenAPI codegen | 타입 안전한 프론트-백 협업 (멀티 플랫폼 지원) |
| React 커스텀 훅 | 프론트엔드 상태 관리 패턴 (크로스플랫폼 공유 로직) |
| localStorage 안전 사용 | 클라이언트 데이터 영속성 (오프라인 지원 기반) |
| 컴포넌트 테스트 | 프론트엔드 품질 보장 |

---

## PR 6: Expo 모바일 UI 구현

### PR 제목
`Expo React Native 모바일 UI 구현`

### 목적
- 웹과 동일한 기능(URL 입력 → 파싱 → 상품 카드 표시)을 모바일 앱으로 구현한다.
- `packages/shared`의 API 클라이언트, 타입, URL 검증을 모바일에서도 재사용하여 코드 공유 효과를 실증한다.
- AsyncStorage로 모바일 영속성을 구현한다.

### 작업 항목

| # | 파일 | 내용 |
|---|------|------|
| 1 | `apps/mobile/hooks/useProducts.ts` | 웹과 동일한 인터페이스의 커스텀 훅. 내부 저장소만 AsyncStorage로 변경. shared의 `createApiClient`, `validateUrl` 사용. |
| 2 | `apps/mobile/hooks/useAsyncStorage.ts` | AsyncStorage 읽기/쓰기 훅. 손상 데이터 graceful 처리. |
| 3 | `apps/mobile/components/UrlInput.tsx` | React Native TextInput + 버튼. 유효성 에러 + 중복 경고. |
| 4 | `apps/mobile/components/ProductCard.tsx` | React Native View. Image + Text 조합. 이미지 로드 실패 시 플레이스홀더. |
| 5 | `apps/mobile/components/ProductList.tsx` | FlatList 기반 세로 스크롤 상품 리스트. 빈 상태 안내. |
| 6 | `apps/mobile/components/ParseProgress.tsx` | 단계별 진행 표시. ActivityIndicator 사용. |
| 7 | `apps/mobile/App.tsx` | 메인 화면: UrlInput + ParseProgress + ProductList 조합. SafeAreaView, StatusBar 적용. |
| 8 | `apps/mobile/app.json` | 앱 이름, 아이콘, 스플래시 설정 업데이트. |

### TDD 순서

```
1. useAsyncStorage 훅 테스트 작성 (RED)
   - 초기값 반환
   - 값 저장 후 재호출 시 저장값 반환
   - 손상 데이터 시 초기값 폴백

2. useAsyncStorage 구현 (GREEN)
   - @react-native-async-storage/async-storage 사용

3. UrlInput 컴포넌트 테스트 작성 (RED)
   - 빈 입력 제출 시 에러 텍스트 표시
   - 정상 URL 제출 시 onSubmit 콜백 호출

4. UrlInput 구현 (GREEN)

5. ProductCard 테스트 작성 (RED)
   - 상품 정보 정상 렌더링

6. ProductCard 구현 (GREEN)

7. ProductList + ParseProgress + App.tsx 조합 (REFACTOR)
```

### 검증 방법
```bash
# 모바일 테스트
cd apps/mobile && npm run test

# Expo 개발 서버
cd apps/mobile && npx expo start
# iOS 시뮬레이터 또는 Expo Go 앱에서:
# 1. URL 입력 → 파싱 → 카드 표시
# 2. 같은 URL 재입력 → 중복 경고
# 3. 앱 재시작 → AsyncStorage에서 복원
# 4. 잘못된 URL → 에러 메시지
```

### 학습 문서
`docs/pr/pr6.html` 작성 -- 아래 개념 정리:
- **React Native vs React DOM**: 렌더링 타겟 차이, View/Text/Image vs div/p/img
- **Expo Router**: 파일 기반 라우팅, Next.js App Router와의 유사점/차이점
- **FlatList**: 대량 목록의 가상화 렌더링, 웹의 무한 스크롤과 비교
- **AsyncStorage**: 모바일 영속성, localStorage와의 차이 (비동기)
- **shared 패키지 재사용**: 타입, API 클라이언트, URL 검증을 웹과 모바일에서 공유하는 실제 효과
- **SafeAreaView, StatusBar**: 모바일 UI 안전 영역 처리
- **Jest + React Native Testing Library**: 모바일 컴포넌트 테스트 방법

### 티니어 면접 매핑
| 학습 내용 | 티니어 요구사항 |
|-----------|----------------|
| React Native | 글로벌 멀티 플랫폼 지원 (iOS/Android 동시 개발) |
| shared 패키지 재사용 | 코드 공유 전략 (개발 효율성, 일관성) |
| AsyncStorage | 모바일 데이터 영속성 (오프라인 대응) |
| FlatList 가상화 | 모바일 성능 최적화 (대규모 데이터 처리) |

---

## PR 7: E2E 테스트 + 마무리 폴리시

### PR 제목
`E2E 테스트 및 반응형 디자인 마무리`

### 목적
- 사용자 관점의 전체 흐름을 E2E 테스트로 검증하여, 실제 서비스 동작 신뢰도를 확보한다.
- 반응형 디자인을 마무리하여 모바일 웹, 태블릿, 데스크톱 모두에서 정상 동작하도록 한다.
- Phase 1의 최종 완성도를 높인다.

### 작업 항목

| # | 파일 | 내용 |
|---|------|------|
| 1 | `apps/web/e2e/happy-path.spec.ts` | Playwright: URL 입력 → 파싱 → 카드 표시 → localStorage 저장 확인. |
| 2 | `apps/web/e2e/error-recovery.spec.ts` | Playwright: 잘못된 URL → 에러 표시 → 올바른 URL 재입력 → 성공. |
| 3 | `apps/web/e2e/partial-parse.spec.ts` | Playwright: OG 파서로 부분 파싱 → 부분 결과 표시 확인. |
| 4 | `apps/web/e2e/multi-products.spec.ts` | Playwright: 여러 URL 추가 → 리스트 누적 확인. |
| 5 | `apps/web/e2e/persistence.spec.ts` | Playwright: 상품 추가 → 페이지 새로고침 → localStorage에서 복원 확인. |
| 6 | `apps/web/playwright.config.ts` | Playwright 설정: baseURL, webServer 설정 (Next.js dev 서버 자동 시작). |
| 7 | `apps/web/src/app/globals.css` | 반응형 디자인 마무리: 모바일(< 640px), 태블릿(640~1024px), 데스크톱(> 1024px) 브레이크포인트. |
| 8 | `apps/web/src/components/ProductCard.tsx` | 수정: 반응형 이미지 크기, 텍스트 줄임 처리. |
| 9 | `apps/web/src/components/ProductList.tsx` | 수정: 반응형 그리드 (모바일 1열, 태블릿 2열, 데스크톱 3열). |

### TDD 순서

```
1. Playwright 설정 (playwright.config.ts)
   - 테스트 디렉토리, baseURL, webServer 설정

2. happy-path.spec.ts 작성 + 실행
   - 백엔드 mock 또는 실제 서버 사용 결정
   - URL 입력 → 버튼 클릭 → 카드 표시 → localStorage 확인

3. error-recovery.spec.ts 작성 + 실행
   - 잘못된 URL → 에러 메시지 확인 → URL 수정 → 성공

4. partial-parse.spec.ts 작성 + 실행
   - 백엔드가 Partial 반환하는 URL 사용 → 부분 정보 표시

5. multi-products.spec.ts 작성 + 실행
   - 3개 URL 순차 추가 → 리스트에 3개 카드 표시

6. persistence.spec.ts 작성 + 실행
   - 상품 추가 → page.reload() → 카드가 여전히 표시되는지

7. 반응형 디자인 수정 (REFACTOR)
   - 각 브레이크포인트에서 E2E 테스트 실행하여 검증
```

### 검증 방법
```bash
# Playwright 설치
cd apps/web && npx playwright install

# E2E 테스트 실행 (백엔드 서버 필요)
cd apps/web && npx playwright test

# 특정 테스트만 실행
npx playwright test happy-path

# 테스트 리포트 확인
npx playwright show-report

# 반응형 검증: 다양한 뷰포트로 E2E 실행
npx playwright test --project=mobile
npx playwright test --project=desktop

# 모바일 스모크 테스트 (수동)
cd apps/mobile && npx expo start
# 1. URL 입력 → 카드 표시
# 2. 앱 재시작 → 데이터 복원
```

### 학습 문서
`docs/pr/pr7.html` 작성 -- 아래 개념 정리:
- **Playwright E2E 테스트**: 설치, 설정, 셀렉터, assertions, 테스트 격리
- **테스트 피라미드**: 단위 → 통합 → E2E의 역할 분담, 각 레벨의 비용/효과
- **Page Object 패턴**: E2E 테스트에서 페이지 구조를 추상화하는 패턴 (선택적)
- **반응형 디자인**: Tailwind CSS 브레이크포인트, 모바일 퍼스트 접근
- **테스트 환경 구성**: 백엔드 mock vs 실제 서버, 테스트 데이터 관리
- **CI에서의 E2E**: 향후 GitHub Actions에서 Playwright 실행하는 방법 (참고)

### 티니어 면접 매핑
| 학습 내용 | 티니어 요구사항 |
|-----------|----------------|
| E2E 테스트 | 안정적 운영 (24/365 장애 없는 환경) |
| 테스트 피라미드 | 테스트 전략 설계 역량 |
| 반응형 디자인 | 멀티 플랫폼 UX (글로벌 서비스 대응) |
| CI/CD 연동 가능성 | 자동화된 품질 관리 파이프라인 |

---

## 전체 의존 관계 및 진행 순서

```
PR 1 (완료) ─── 프로젝트 초기 세팅
  │
  ├── PR 2 ─── 도메인 포트 이동 + OG 파서
  │     │
  │     └── PR 3 ─── 어필리에이트 API + Fallback Chain
  │           │
  │           └── PR 4 ─── API 엔드포인트 + 이미지 프록시
  │                 │
  │                 ├── PR 5 ─── Shared 타입 동기화 + Web UI
  │                 │     │
  │                 │     └── PR 7 ─── E2E 테스트 + 폴리시 (웹)
  │                 │
  │                 └── PR 6 ─── Expo 모바일 UI
  │                       │
  │                       └── PR 7 ─── E2E 테스트 + 폴리시 (모바일 스모크)
```

- PR 2 → 3 → 4: 백엔드 순차 (각 PR이 이전 PR의 코드에 의존)
- PR 5, 6: 백엔드 완성(PR 4) 후 병렬 진행 가능
- PR 7: PR 5, 6 모두 완료 후 진행

## PR별 예상 코드 규모

| PR | 파일 수 | 예상 줄 수 | 비고 |
|----|---------|-----------|------|
| PR 2 | 7 (본체 4 + 테스트 2 + 이동 1) | ~150줄 | 포트 이동은 패키지 선언 변경만 |
| PR 3 | 8 (본체 4 + 테스트 3 + 설정 1) | ~200줄 | 테스트가 가장 큰 비중 |
| PR 4 | 8 (본체 5 + 테스트 2 + 핸들러 1) | ~180줄 | Presentation 레이어 전체 |
| PR 5 | 10 (컴포넌트 5 + 훅 2 + 페이지 2 + 타입 1) | ~200줄 | 프론트엔드 핵심 UI |
| PR 6 | 8 (컴포넌트 4 + 훅 2 + App 1 + 설정 1) | ~180줄 | 웹과 구조 유사 |
| PR 7 | 9 (E2E 5 + 설정 1 + 스타일 수정 3) | ~200줄 | E2E 테스트 위주 |

## 티니어 면접 준비 종합 매핑

| 티니어 요구사항 | 관련 PR |
|----------------|---------|
| Kotlin/Spring 기반 서버 개발 | PR 2, 3, 4 |
| 안정적 운영 (24/365) | PR 3 (장애 격리), PR 7 (E2E) |
| 외부 API 연동 + 장애 대응 | PR 2 (OG 파서), PR 3 (어필리에이트 API + Fallback) |
| 캐시 전략 | PR 3 (@Cacheable + Caffeine) |
| REST API 설계 | PR 4 (엔드포인트 + 검증 + 에러 처리) |
| 글로벌 멀티 플랫폼 지원 | PR 5 (웹), PR 6 (모바일), 공유 패키지 |
| 테스트 + 코드 품질 | 전체 PR (TDD) |
| 빠르게 만들고 빠르게 검증 | PR 단위 100~200줄, E2E로 즉시 검증 |
