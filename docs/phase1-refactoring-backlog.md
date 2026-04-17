# Phase 1 리팩토링 / 커버리지 백로그

> Phase 1(PR 1~7) 완료 후 코드 검토에서 도출된 리팩토링·테스트 추가 항목 중
> **백엔드 외 영역**(Shared / Web / Mobile)의 항목을 보관한다. Phase 2 진입 후
> 새 기능 개발과 자연스럽게 합쳐서 처리하거나, 별도 세션에서 착수한다.
>
> 백엔드 항목은 별도 트랙(`feature/pr-be1*` ~ `feature/pr-be5*`)에서 선행 처리한다.

---

## 1. Shared (3 PR)

### BL-S1: 테스트 채우기 + validateUrl/baseUrl 리팩토링 + types 단순화

- **우선순위**: HIGH
- **예상 diff**: ~250줄
- **내용**:
  - `packages/shared/src/validation/url.ts` 테스트 ~12 케이스 (빈/공백/프로토콜 누락/javascript:/ftp:/유니코드 등)
  - `packages/shared/src/api/client.ts` 테스트 ~9 케이스 (HTTP 200/400/401/500/503/timeout/fetch throw/non-JSON/json() 실패)
  - `validation/url.ts`: 정규식 + `new URL()` 검증 중복 제거 → `new URL()` 단독 사용
  - `api/client.ts`: `baseUrl = ""` 기본값 제거 또는 명시적 전달 강제 (환경변수 읽기 로직 추가 검토)
  - `types/product.ts`: `Omit<ApiParseResponse, ...> & Product & {...}` 합성을 인라인 정의로 단순화 (또는 주석으로 의도 설명)
- **검증**: `cd packages/shared && npm run test`

### BL-S2: useStorage 훅 도입 (Storage 추상화)

- **우선순위**: HIGH
- **예상 diff**: ~150줄
- **내용**:
  - `packages/shared/src/storage/useStorage.ts`: 플랫폼 중립 훅 (adapter 주입 방식)
  - `packages/shared/src/storage/adapter.ts`: `StorageAdapter` 인터페이스 (`getItem`/`setItem`/`removeItem`)
  - 기본 구현: 웹용 `createLocalStorageAdapter`, 모바일용 `createAsyncStorageAdapter`
  - hydration/손상 데이터 graceful 처리 공통화
  - 테스트 포함
- **후속**: BL-W4 (Web useLocalStorage 교체), BL-M3 (Mobile useAsyncStorage 교체)

---

## 2. Web (4 PR)

### BL-W1: useProducts 훅 테스트

- **우선순위**: HIGH
- **예상 diff**: ~250줄 (테스트 + 피드백 상수화)
- **내용**:
  - `apps/web/src/hooks/useProducts.test.tsx` 신규 (~5-6 케이스):
    - 중복 URL 체크 → warning phase
    - API 실패(result.ok === false) → error phase
    - 성공 → products 배열 prepend
    - 유효성 실패 → error phase (no API 호출)
    - submitUrl 반환값 검증
  - `apps/web/src/lib/messages.ts` 추출: `"상품 카드를 리스트에 추가했습니다."` 등 피드백 문자열 상수화 (i18n 대비)
- **참조**: `apps/web/src/hooks/useProducts.ts:28-74`

### BL-W2: 컴포넌트 테스트 + 작은 리팩토링

- **우선순위**: HIGH
- **예상 diff**: ~250줄
- **내용**:
  - `ProductList.test.tsx` 신규: hydration 미완료 / 빈 상태 / 여러 카드 렌더링 (3 케이스)
  - `ParseProgress.test.tsx` 신규: idle/validating/parsing/success/error 상태별 표시 (5 케이스)
  - `ParseProgress.tsx` 상단에 `"use client"` 명시 (현재 누락)
  - `useProducts.buildImageSrc` → `apiClient.imageProxyUrl`로 일원화(이미 shared에 있음)

### BL-W3: E2E 추가 (중복 URL, 502 실패 복구)

- **우선순위**: MEDIUM
- **예상 diff**: ~100줄
- **내용**:
  - `apps/web/e2e/duplicate-url.spec.ts` 신규: 같은 URL 두 번 입력 → "이미 추가한 링크입니다" 경고
  - `apps/web/e2e/api-failure.spec.ts` 신규: 502 응답 → 에러 메시지 → 다른 URL로 복구
  - `helpers.ts`에 실패 응답 mock 시나리오 추가

### BL-W4: useStorage 훅 교체 (BL-S2 의존)

- **우선순위**: HIGH (하지만 BL-S2 선행 필요)
- **예상 diff**: ~100줄
- **내용**:
  - `useLocalStorage` 제거 → shared의 `useStorage` 사용
  - `useProducts`, `useLocalStorage.test.tsx` 경로 조정

---

## 3. Mobile (5 PR)

### BL-M1: 컴포넌트 테스트

- **우선순위**: HIGH
- **예상 diff**: ~250줄
- **내용**:
  - `ProductCard.test.tsx` 확장: 이미지 로드 실패 → 플레이스홀더 (onError 시뮬레이션)
  - `ProductList.test.tsx` 신규: FlatList 렌더링, 빈 상태, hydration 미완료
  - `ParseProgress.test.tsx` 신규: 상태별 스타일/ActivityIndicator 표시
- **참조**: `apps/mobile/components/ProductCard.tsx:30` imageFailed 상태

### BL-M2: useProducts 훅 테스트

- **우선순위**: HIGH
- **예상 diff**: ~150줄
- **내용**:
  - `apps/mobile/hooks/useProducts.test.tsx` 신규 (~5 케이스):
    - 유효성 실패 / 중복 / API 실패 / 성공 / AsyncStorage 통합
  - API 클라이언트 mock 전략 결정 (msw-native or 수동 fetch mock)

### BL-M3: useStorage 교체 + 네이밍 통일 (BL-S2 의존)

- **우선순위**: HIGH (BL-S2 선행)
- **예상 diff**: ~150줄
- **내용**:
  - `useAsyncStorage` 제거 → shared `useStorage` 사용
  - `buildImageUri` → `buildImageSrc` 로 통일 (웹과 동일 네이밍)
  - `useProducts` 호출부 수정

### BL-M4: 테마/색상 토큰 분리

- **우선순위**: LOW
- **예상 diff**: ~200줄
- **내용**:
  - `apps/mobile/theme/colors.ts` 또는 `tokens.ts` 신규
  - `App.tsx:70-163` StyleSheet 내 하드코딩 색상(#091525, #67e8f9 등 11개) 상수로 추출
  - `UrlInput.tsx:13-29` noticeColors 인라인 객체 → 공용 테마로 이동
  - 다크모드 대비 구조 열어두기 (실제 다크모드 구현은 Phase 2 이상)

### BL-M5: API 엔드포인트 환경 분리 + 에러 분류

- **우선순위**: LOW
- **예상 diff**: ~300줄
- **내용**:
  - `.env.development`, `.env.staging`, `.env.production` 분리
  - `app.json`의 `extra` 필드로 환경별 API URL 관리 (또는 EAS build profile)
  - `useProducts.ts:7-14` Platform.select 로직을 환경 변수 기반으로 교체
  - `packages/shared`에 `ParseErrorKind` 타입 추가 (`network`/`timeout`/`invalid_url`/`duplicate`/`parse_failed`)
  - `Feedback` 타입에 `kind` 추가, 훅 레벨 에러 매핑 세분화

---

## 선행 의존 관계

```
BL-S1 (Shared 테스트+리팩토링)  ── 독립
BL-S2 (useStorage 훅) ──┬── BL-W4 (Web 교체)
                        └── BL-M3 (Mobile 교체)

BL-W1, BL-W2, BL-W3 ── 독립 (Web)
BL-M1, BL-M2 ── 독립 (Mobile)
BL-M4, BL-M5 ── 독립 (Mobile LOW)
```

## 처리 방침

- **Phase 2 기능 개발과 자연스럽게 묶이는 항목 우선**: 예를 들어 Phase 2에서 회원관리를 붙일 때 Web useProducts를 어차피 리팩토링하게 되면 BL-W1 테스트도 같이 진행.
- **BL-S2(useStorage)는 Phase 2 플랫폼 확장(웹뷰 등) 또는 테마·다국어 도입 같은 공통 인프라 작업과 함께 처리**.
- **LOW(BL-M4, BL-M5)는 모바일 배포가 실제 일정에 올라올 때 함께**.

Phase 2 착수 시 `/dev-plan`에서 이 문서를 입력으로 받아 새 기능과 섞어 재분할한다.
