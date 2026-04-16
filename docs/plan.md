# Phase 1 구현 계획: 상품 링크 파서 앱

> **NOTE**: 이 문서는 초기 Python/FastAPI 기반으로 작성된 계획입니다. 백엔드는 Kotlin/Spring Boot로 전환되었습니다. 현재 아키텍처는 `CLAUDE.md`와 `docs/architecture.html`을 참고하세요.

## Context

쇼핑몰 링크를 입력하면 상품명, 가격, 이미지를 파싱하여 보여주는 크로스플랫폼 앱의 MVP 1단계.
폴센트처럼 쇼핑몰 상품 정보를 수집·표시하는 서비스를 목표로 하며, 이후 회원관리(2단계)와 공유 기능(3단계)으로 확장 예정.

## 기술 스택

| 영역 | 기술 | 비고 |
|------|------|------|
| Web | Next.js 15 (App Router, TypeScript) | SSR, 최적의 웹 경험 |
| Mobile | Expo (React Native, TypeScript) | iOS/Android 동시 지원 |
| Backend | FastAPI (Python 3.12+) | async, 클린 아키텍처 |
| DB | Phase 1에서는 불필요 (stateless) | 프론트 로컬 저장소만 사용 |
| Monorepo | Turborepo | TS 앱 간 코드 공유 |
| Testing | pytest (백엔드), Vitest + RTL (웹), Jest + RNTL (모바일) |
| 타입 동기화 | OpenAPI codegen | FastAPI spec → TS 타입 자동 생성 |

## 아키텍처 원칙

- **Clean Architecture**: Domain → Application → Infrastructure → Presentation
- **DDD**: Product 엔티티, Value Object (Money)
- **TDD**: 테스트 먼저 작성 → 최소 구현 → 리팩토링
- **Layered Architecture**: 의존성은 안쪽(Domain)으로만 향함

## Phase 1 동작 방식

1. 사용자가 쇼핑몰 URL을 입력
2. 프론트엔드: 로컬 리스트에서 중복 URL 체크 → 중복이면 경고
3. 프론트엔드 → 백엔드 API 호출 (`POST /api/v1/products/parse`)
4. 백엔드: TTL 캐시 확인 → 캐시 히트면 즉시 반환
5. 백엔드: 어필리에이트 API(쿠팡/11번가) 또는 OG 파서로 상품 정보 파싱
6. 프론트엔드: 단계별 진행 표시 → 결과를 화면에 표시 + 로컬 저장소에 저장
7. 브라우저/앱을 닫으면 데이터 소멸 (Phase 2에서 영구 저장)

## 프로젝트 구조

```
linkcart/                       # ~/git/linkcart (GitHub: park-j3onghoon/linkcart)
├── backend/                    # FastAPI (Python)
│   ├── app/
│   │   ├── domain/
│   │   │   ├── entities/
│   │   │   │   └── product.py          # Product 엔티티 (모든 필드 required)
│   │   │   └── value_objects/
│   │   │       └── money.py            # 가격 VO (금액 + 통화)
│   │   ├── application/
│   │   │   ├── ports/
│   │   │   │   ├── product_parser.py   # ProductParser Protocol (인터페이스)
│   │   │   │   └── product_repository.py # ProductRepository Protocol (Phase 2용 Port만)
│   │   │   ├── dto/
│   │   │   │   └── parse_result.py     # ParseResult DTO (부분 파싱 표현)
│   │   │   └── use_cases/
│   │   │       └── parse_product.py    # 파싱 유스케이스 + fallback chain
│   │   ├── infrastructure/
│   │   │   ├── clients/
│   │   │   │   ├── coupang_api.py      # 쿠팡 Seller Open API(HMAC) 클라이언트
│   │   │   │   └── elevenst_api.py     # 11번가 Open API 클라이언트
│   │   │   ├── parsers/
│   │   │   │   ├── og_parser.py        # Open Graph 범용 파서 (폴백)
│   │   │   │   └── factory.py          # URL → 적절한 파서/API 선택
│   │   │   └── cache.py               # TTL 인메모리 캐시
│   │   └── presentation/
│   │       ├── api/v1/
│   │       │   ├── products.py         # 상품 파싱 API 라우터
│   │       │   └── images.py           # 이미지 프록시 엔드포인트
│   │       └── schemas/
│   │           └── product.py          # Pydantic 요청/응답 스키마
│   ├── tests/
│   │   ├── unit/                       # 파서/API 클라이언트 단위 테스트
│   │   ├── integration/                # API 통합 테스트
│   │   └── live/                       # 실제 사이트 검증 (CI 주 1회)
│   ├── main.py
│   └── pyproject.toml
├── apps/
│   ├── web/                            # Next.js
│   │   └── src/
│   │       ├── app/                    # App Router 페이지
│   │       ├── components/             # UI 컴포넌트
│   │       ├── hooks/                  # 커스텀 훅
│   │       └── lib/                    # 유틸리티
│   └── mobile/                         # Expo (React Native)
│       ├── app/                        # Expo Router 페이지
│       ├── components/
│       └── hooks/
├── packages/
│   └── shared/                         # 공유 TS 패키지
│       └── src/
│           ├── types/                  # OpenAPI codegen으로 자동 생성
│           ├── api/                    # API 클라이언트 (ApiResult<T> 반환)
│           └── validation/             # URL 유효성 검증
├── package.json                        # Turborepo 루트
└── turbo.json
```

## 백엔드 상세 설계

### Domain Layer

```python
# Product 엔티티 — 모든 필드 required (깨끗하게 유지)
@dataclass(frozen=True)
class Product:
    name: str
    price: Money
    image_url: str
    source_url: str
    mall: str           # "coupang" | "11st" | "generic"

# Money Value Object
@dataclass(frozen=True)
class Money:
    amount: int         # 원 단위 정수
    currency: str       # "KRW"
```

### Application Layer

```python
# Parser Port (Protocol)
class ProductParser(Protocol):
    async def parse(self, url: str) -> Product: ...
    def can_parse(self, url: str) -> bool: ...

# ParseResult DTO — 부분 파싱 표현 (Domain 엔티티와 분리)
@dataclass
class ParseResult:
    product: Product | None          # 전체 성공 시 Product
    partial: dict[str, Any]          # 부분 파싱된 필드
    parser_used: str                 # "coupang_api" | "elevenst_api" | "og"
    fallback_used: bool              # OG 폴백 사용 여부

# ProductRepository Port (Phase 2용 — 구현체 없이 Protocol만)
class ProductRepository(Protocol):
    async def save(self, product: Product) -> None: ...
    async def find_by_url(self, url: str) -> Product | None: ...
```

### Infrastructure Layer — 파싱 전략 (리뷰 후 변경)

| 클라이언트/파서 | 대상 | 방식 |
|----------------|------|------|
| CoupangApiClient | coupang.com | **쿠팡 Seller Open API(HMAC)** |
| ElevenStApiClient | 11st.co.kr | **11번가 Open API** (어필리에이트) |
| OGParser | 모든 URL (폴백) | Open Graph meta 태그 파싱 (httpx + BS4) |
| ParserFactory | URL 라우팅 | URL 도메인 기반으로 API 클라이언트 또는 OG 파서 선택 |

**Playwright 불필요** — 어필리에이트 API를 사용하므로 HTML 스크래핑이 기본 전략이 아님.

**파싱 흐름 (Fallback Chain)**:
```
URL → ParserFactory.get_parser(url)
  → 쿠팡 URL? → CoupangApiClient → 성공: Product / 실패: OGParser 폴백
  → 11번가 URL? → ElevenStApiClient → 성공: Product / 실패: OGParser 폴백
  → 기타 URL? → OGParser → 성공: Product / 부분 성공: ParseResult.partial / 실패: 에러
```

**중복 방지 (TTL 캐시)**:
```python
from cachetools import TTLCache
cache = TTLCache(maxsize=100, ttl=300)  # 5분

async def parse_product(url):
    if url in cache:
        return cache[url]
    result = await _do_parse(url)
    cache[url] = result
    return result
```

### URL 유효성 검증 — 레이어별 책임 분리

| 레이어 | 검증 내용 |
|--------|-----------|
| Presentation (API) | URL 형식 검증 (Pydantic `HttpUrl`) |
| Application (UseCase) | 지원 쇼핑몰 여부 확인 |
| Infrastructure (Parser) | 기술적 파싱 가능성 (API 응답, HTML 구조) |

### API 스키마

```
POST /api/v1/products/parse
Request:  { "url": "https://www.coupang.com/..." }
Response: {
  "name": "상품명",
  "price": { "amount": 15900, "currency": "KRW" },
  "image_url": "https://...",
  "source_url": "https://www.coupang.com/...",
  "mall": "coupang",
  "partial": null,                    # 부분 파싱 시 { "name": "...", ... }
  "parser_used": "coupang_api",
  "fallback_used": false
}

GET /api/v1/images/proxy?url=...      # OG 파서 이미지용 프록시 (어필리에이트 API 이미지는 직접 사용)
Response: image bytes (Content-Type 전달)
```

## 프론트엔드 상세 설계

### 공유 패키지 (packages/shared)

- **타입**: OpenAPI codegen으로 백엔드 스키마에서 자동 생성 (`npx openapi-typescript`)
- **API 클라이언트**: fetch 기반, `ApiResult<T>` 타입 반환
- **URL 검증**: `validateUrl(url)` — 형식 검증

```typescript
// API 클라이언트 에러 처리 패턴 — Result 타입
type ApiResult<T> =
  | { ok: true; data: T }
  | { ok: false; error: ApiError }

type ApiError =
  | { code: 'NETWORK'; message: string }
  | { code: 'PARSE_FAILED'; message: string }
  | { code: 'TIMEOUT'; message: string }
  | { code: 'UNKNOWN'; message: string }
```

### UI 구성 (Web & Mobile 공통 구조)

1. **URL 입력 영역**: 텍스트 입력 + "추가" 버튼 + 중복 URL 경고
2. **단계별 진행 표시**: "URL 확인 중..." → "상품 정보 파싱 중..." → "완료"
3. **상품 카드**: 이미지 + 상품명 + 가격 + 출처 쇼핑몰 뱃지
4. **상품 리스트**: 카드 그리드 (웹: 반응형 그리드, 모바일: 세로 스크롤)
5. **에러/빈 상태**: 이미지 실패 → 플레이스홀더, 빈 리스트 → 안내 UI

### 로컬 저장

- **Web**: `localStorage` (JSON 직렬화)
- **Mobile**: `AsyncStorage` (React Native)
- 저장소 불가 시 (시크릿 모드 등) graceful degradation

### 프론트 중복 URL 체크

```typescript
const existing = products.find(p => p.sourceUrl === url);
if (existing) {
  // 이미 추가된 상품 경고
  return;
}
```

## 구현 순서 (PR 단위)

### PR 1: 프로젝트 초기 세팅
- GitHub repo clone (`~/git/linkcart`)
- `docs/plan.md` — 이 구현 계획을 프로젝트에 포함
- Turborepo 모노레포 초기화
- Next.js 앱 스캐폴딩
- Expo 앱 스캐폴딩
- FastAPI 백엔드 스캐폴딩
- shared 패키지 세팅
- OpenAPI codegen 스크립트 설정
- CI: lint, type check 기본 설정

### PR 2: 백엔드 Domain + OG 파서 (TDD)
- Product 엔티티, Money VO, ParseResult DTO
- ProductParser Protocol, ProductRepository Protocol (인터페이스만)
- OGParser 구현 (범용 폴백 파서)
- ParserFactory 기본 구조
- 단위 테스트: 정상 + 부분 OG + OG 없음 + 비HTML 응답
- TTL 캐시 구현 + 테스트

### PR 3: 백엔드 어필리에이트 API 클라이언트 (TDD)
- CoupangApiClient (쿠팡 Seller Open API(HMAC))
- ElevenStApiClient (11번가 Open API)
- 각 클라이언트 단위 테스트 (mock API 응답)
- 에러 케이스 테스트: API 실패, 타임아웃, 상품 삭제됨, 품절
- Fallback chain 통합 테스트 (5개 시나리오: 전체성공/폴백성공/부분성공/전체실패/타임아웃)

### PR 4: 백엔드 API 엔드포인트 + 이미지 프록시
- `POST /api/v1/products/parse` 라우터
- `GET /api/v1/images/proxy` 엔드포인트 (OG 파서 이미지용)
- Pydantic 요청/응답 스키마
- 레이어별 URL 검증 (형식 → 지원몰 → 파싱가능성)
- 에러 응답 형식 통일 (파싱 실패, 타임아웃, 검증 에러)
- 통합 테스트 (httpx TestClient)

### PR 5: Shared 패키지 + Web UI
- OpenAPI codegen으로 TS 타입 자동 생성
- API 클라이언트 (`ApiResult<T>` 반환)
- Next.js: URL 입력 → 중복 체크 → 파싱 → 단계별 진행 → 카드 표시
- localStorage 저장/복원 (손상 데이터 graceful 처리)
- 웹 컴포넌트 테스트: 이미지 실패→플레이스홀더, 빈 상태 UI, 로딩 중 중복 제출 방지

### PR 6: Mobile UI
- Expo: URL 입력 → 중복 체크 → 파싱 → 단계별 진행 → 카드 표시
- AsyncStorage 저장/복원
- 모바일 UI 최적화

### PR 7: E2E 테스트 + 폴리시
- **웹 E2E** (5개 흐름):
  1. Happy path: URL → 파싱 → 카드 표시 → 저장
  2. 에러 복구: 잘못된 URL → 에러 → 올바른 URL → 성공
  3. 부분 파싱: OG 파서 → 부분 결과 표시
  4. 다중 상품: 여러 URL 추가 → 리스트 누적
  5. 영속성: 상품 추가 → 새로고침 → localStorage에서 복원
- **모바일 스모크** (2개):
  1. Happy path: URL → 파싱 → 카드 표시
  2. 저장소 복원: 앱 재시작 → AsyncStorage에서 복원
- 반응형 디자인 마무리

## 테스트 전략 요약

| 테스트 레벨 | 도구 | 대상 |
|-------------|------|------|
| 단위 (백엔드) | pytest | 파서, API 클라이언트, 유스케이스, 캐시 |
| 단위 (프론트) | Vitest/Jest + RTL/RNTL | 컴포넌트 렌더링, 에러 상태, 상호작용 |
| 통합 (백엔드) | pytest + httpx TestClient | API 엔드포인트 전체 흐름 |
| 통합 (UseCase) | pytest | Fallback chain 5개 시나리오 |
| E2E (웹) | Playwright | 5개 사용자 흐름 |
| E2E (모바일) | Detox 또는 Maestro | 2개 스모크 테스트 |
| Live 검증 | pytest (CI 주 1회) | 실제 어필리에이트 API + OG 파서 동작 확인 |

## 검증 방법

### 백엔드
```bash
cd backend
pytest                              # 전체 테스트
pytest tests/unit/                  # 단위 테스트만
pytest tests/integration/           # 통합 테스트만
uvicorn main:app --reload           # 로컬 서버 실행 후 /docs 에서 API 테스트
```

### 프론트엔드
```bash
# 타입 동기화
cd packages/shared && npm run codegen   # OpenAPI → TS 타입 생성

# 웹
cd apps/web
npm run dev                         # 로컬 개발 서버
npm run test                        # 컴포넌트 테스트

# 모바일
cd apps/mobile
npx expo start                      # Expo 개발 서버
npm run test                        # 컴포넌트 테스트
```

### E2E 검증 시나리오
1. 쿠팡 상품 URL → 어필리에이트 API → 상품명/가격/이미지 정상 표시
2. 11번가 상품 URL → 어필리에이트 API → 상품명/가격/이미지 정상 표시
3. 임의 쇼핑몰 URL → OG 파서 폴백 → 기본 정보 표시
4. 잘못된 URL → 에러 메시지 표시 → 올바른 URL 재입력 → 성공
5. 같은 URL 재입력 → 중복 경고 표시
6. 여러 상품 추가 → 리스트에 누적 표시
7. 브라우저 새로고침 → localStorage에서 복원 (웹)
8. 앱 재시작 → AsyncStorage에서 복원 (모바일)

## Plan Review 결과 반영 사항

| # | 이슈 | 결정 |
|---|------|------|
| 1 | 쿠팡 파싱 전략 | ~~Playwright~~ → 어필리에이트 API |
| 2 | 에러 전략 | Fallback chain (API → OG 폴백 → partial 허용) |
| 3 | 타입 동기화 | OpenAPI codegen (FastAPI spec → TS) |
| 4 | 미사용 레이어 | Port(Protocol)만 유지, 구현체는 Phase 2 |
| 5 | Partial 표현 | ParseResult DTO (Domain 엔티티 깨끗 유지) |
| 6 | 에러 패턴 | ApiResult<T> (Result 타입) |
| 7 | Mock 관리 | API mock + 주기적 live 검증 (CI 주 1회) |
| 8 | URL 검증 | 레이어별 책임 분리 |
| 9 | Fallback 테스트 | UseCase 통합 테스트 (5개 시나리오) |
| 10 | 에러 케이스 | 파서별 에러 fixture (품절/삭제/타임아웃) |
| 11 | FE 엣지 테스트 | 핵심 3개 (이미지실패, 빈상태, 중복제출) |
| 12 | E2E 범위 | 웹 5개 흐름 + 모바일 스모크 2개 |
| 13 | 파싱 UX | 단계별 진행 표시 |
| 14 | 파싱 방식 | 어필리에이트 API 우선 + OG 폴백 |
| 15 | 중복 방지 | 프론트 체크 + 백엔드 TTL 캐시 |
| 16 | 이미지 처리 | OG 파서 이미지만 백엔드 프록시, API 이미지는 직접 사용 |
