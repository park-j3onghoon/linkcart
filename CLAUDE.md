# LinkCart

쇼핑몰 링크를 입력하면 상품명, 가격, 이미지를 파싱하여 보여주는 크로스플랫폼 앱.

## 기술 스택

- **Web**: Next.js 15 (App Router, TypeScript, Tailwind CSS) — `apps/web/`
- **Mobile**: Expo + React Native (TypeScript) — `apps/mobile/`
- **Backend**: FastAPI (Python 3.10+) — `backend/`
- **Shared**: TypeScript 타입, API 클라이언트, URL 검증 — `packages/shared/`
- **Monorepo**: Turborepo
- **파싱**: 어필리에이트 API(쿠팡/11번가) + OG 태그 폴백

## 아키텍처

### 백엔드 클린 아키텍처 (의존성: 안쪽으로만)
```
Domain (entities, value_objects)
  ↑
Application (ports/Protocol, dto, use_cases)
  ↑
Infrastructure (clients/, parsers/, cache)
  ↑
Presentation (api/v1/, schemas/)
```

### 프론트엔드 공유 전략
- `packages/shared`에서 타입, API 클라이언트(`ApiResult<T>`), URL 검증 공유
- UI 컴포넌트는 웹/모바일 각각 구현 (렌더링 타겟이 다름)
- OpenAPI codegen으로 백엔드 스키마 → TS 타입 자동 생성

## 개발 명령어

```bash
# 모노레포 전체
npm run dev          # 전체 dev 서버
npm run build        # 전체 빌드
npm run lint         # 전체 린트
npm run test         # 전체 테스트

# 백엔드
cd backend
source .venv/bin/activate
uvicorn main:app --reload          # 서버 (localhost:8000, /docs에서 Swagger UI)
pytest                              # 테스트
pytest --cov=app                    # 커버리지

# 웹
cd apps/web
npm run dev                         # localhost:3000

# 모바일
cd apps/mobile
npx expo start                      # Expo 개발 서버

# 타입 동기화
cd packages/shared
npm run codegen                     # 백엔드 서버 실행 중일 때
```

## 코딩 컨벤션

### Python (백엔드)
- Python 3.10+ 문법 사용 (`X | Y` 유니온 타입, `match` 문 등)
- 타입 힌트 필수 (mypy strict)
- ruff로 린트/포맷
- import는 파일 최상단에 배치 (함수 내부 import 금지)
- 테스트 메서드명은 영어, docstring만 한글
- update/delete 미존재 시 None 대신 도메인 예외 raise, find는 None OK
- 프레임워크 빌트인 동작은 테스트하지 않음 (커스텀 로직만)
- 데이터 객체 단독 테스트 불필요, 스냅샷 테스트 금지

### TypeScript (프론트엔드)
- strict 모드
- ESLint + Prettier
- React 컴포넌트는 함수형 + hooks

### 공통
- TDD: 테스트 먼저 → 최소 구현 → 리팩토링
- 커밋 메시지/PR 제목은 한글
- PR은 100~200줄 단위 권장

## 파싱 전략

```
URL → ParserFactory
  → 쿠팡 URL → CoupangApiClient (어필리에이트 API) → 실패 시 OG 폴백
  → 11번가 URL → ElevenStApiClient (어필리에이트 API) → 실패 시 OG 폴백
  → 기타 URL → OGParser (Open Graph 태그)
```

- Fallback chain: 전용 API → OG 파서 → partial 허용
- 부분 파싱은 `ParseResult` DTO로 표현 (Domain `Product` 엔티티는 모든 필드 required 유지)
- 백엔드 TTL 캐시 (5분) + 프론트 중복 URL 체크로 이중 방지

## 계획 문서

- `docs/plan.md` — Phase 1 구현 계획 (16개 리뷰 이슈 반영)
