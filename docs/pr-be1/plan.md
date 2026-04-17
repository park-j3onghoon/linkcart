# PR-BE1: 파서 엣지 케이스 테스트 추가

## 목적

Phase 1 테스트 커버리지 공백 메우기. 파서 3종(Coupang/11st/OG)의 실패·경계 분기를 테스트로 고정한다. **프로덕션 코드 변경 0**.

## 브랜치

`feature/pr-be1-parser-edge-tests` (origin/main 기준)

## 테스트 13개 (plan-review 반영 후)

### CoupangApiClientTest (+5)

1. `empty response body returns Failure` — `"쿠팡 API 응답이 비어 있습니다"`
2. `invalid JSON returns Failure` — `"쿠팡 API 응답 파싱 실패"`
3. `non-SUCCESS code returns Failure with server message` — code ≠ "SUCCESS" + data null
4. `missing price returns Failure` — salePrice null → `"쿠팡 상품 가격이 없습니다"`
5. `empty data returns Failure` *(Issue 21 — data=null 또는 primaryItem=null)* — `"쿠팡 API 응답에 상품 정보가 없습니다"`

### ElevenStApiClientTest (+4)

6. `empty XML body returns Failure` — `"11번가 API 응답이 비어 있습니다"`
7. `deleted product returns Failure` — `ProductStatus=DELETED`
8. `non-200 resultCode returns Failure with result message`
9. `sold out product returns Failure` *(Issue 22)* — `ProductStatus=SOLDOUT` → `"11번가 상품이 품절되었습니다"`

### OgParserTest (+4)

10. `price without currency returns Success with KRW` *(Issue 18 네이밍 수정)* — og:price:currency 미지정 → Money.currency == "KRW"
11. `non-numeric price returns Partial` — og:price:amount 숫자 아님 → priceAmount null → Partial
12. `image-only content returns Partial with imageUrl`
13. `html without parseable content returns Failure` *(Issue 24)* — HTML은 있으나 OG/title/image/price 전부 부재 → `"파싱 가능한 정보가 없습니다"`

## plan-review 반영 규칙

### Issue 17: Helper 함수 재구성 (시나리오별 분리)

`successBody()`에 boolean flag를 계속 추가하는 대신, 시나리오별 helper로 분리한다.

**CoupangApiClientTest.kt**:
- `successBody(name, price = 123000L)` — 정상 응답만
- `deletedBody(name)` — 삭제 상태 응답
- `soldOutBody(name)` — 품절 응답 (maximumBuyCount=0)
- `missingPriceBody(name)` — 가격 없는 응답
- `failureCodeBody(code, message)` — code ≠ SUCCESS + data null 응답
- `emptyDataBody()` — data 없거나 primaryItem 없는 응답

기존 테스트(`deleted product returns Failure`, `sold out product returns Failure`)도 이 helper를 쓰도록 함께 정리.

**ElevenStApiClientTest.kt**:
- `successBody(name)` — 정상 응답
- `statusBody(name, status)` — `ProductStatus` 지정 (DELETED/SOLDOUT 등)
- `resultErrorBody(resultCode, resultMessage)` — non-200 resultCode

### Issue 19: assert 메시지 검증 규칙

- 고정 문자열(우리 코드가 내뱉는 메시지): `assertEquals(expected, result.reason)`
- 외부 서버 메시지 포함: `assertTrue(result.reason.contains(serverMessage))`
- 즉, 테스트 #3(Coupang non-SUCCESS code)와 #8(11st non-200 resultCode)만 `contains`, 나머지는 전부 `assertEquals`.

### Issue 20: 에러 메시지 상수화 스코프 아웃

에러 메시지 문자열을 프로덕션 상수로 추출하는 것은 **이 PR 범위 밖**. 현 literal을 그대로 assert한다. 메시지 상수화는 별도 리팩토링 PR에서 처리.

## NOT in scope

- **name/image missing 테스트 (Coupang/11st)** *(Issue 23)* — Phase 2 "파서 validation 파라미터라이즈드 통합" PR에서 한 번에 처리
- **에러 메시지 상수화** — 별도 PR
- **SafeUrlChecker 단위 테스트** — PR-BE2
- **accessKey/apiKey blank, extractProductId null** — private 분기, 간접 커버로 충분

## What already exists (기존 테스트 14개)

- Coupang: Success / HTTP 404 / timeout / deleted / sold out
- 11st: Success / HTTP 500 / timeout / missing product
- OG: canParse=true / full OG Success / title-only Partial / title-tag fallback / empty HTML Failure

## TDD 순서

1. CoupangApiClientTest: helper 재구성 → 신규 5개 테스트 작성 → `./gradlew test --tests CoupangApiClientTest` 통과
2. ElevenStApiClientTest: helper 재구성 → 신규 4개 테스트 → `./gradlew test --tests ElevenStApiClientTest` 통과
3. OgParserTest: 신규 4개 테스트 → `./gradlew test --tests OgParserTest` 통과
4. 전체 백엔드 테스트: `./gradlew test` 통과 (기존 테스트가 helper 재구성에 영향받지 않는지 검증)

## 파일 변경

- `backend/src/test/kotlin/com/linkcart/infrastructure/adapter/parser/CoupangApiClientTest.kt`
- `backend/src/test/kotlin/com/linkcart/infrastructure/adapter/parser/ElevenStApiClientTest.kt`
- `backend/src/test/kotlin/com/linkcart/infrastructure/adapter/parser/OgParserTest.kt`

예상 diff: ~250줄 (helper 재구성 포함)

## 검증

```bash
cd backend && ./gradlew test
```

모든 기존 + 신규 테스트 통과 확인.

## Completion summary

- Step 0: 스코프 챌린지 (BIG CHANGE)
- Dimensions: 3/6 active (Architecture, Coding Standards, Test Coverage)
- Architecture: 0 이슈
- Coding Standards: 4 이슈 (전부 B 반영)
- Test Coverage: 4 이슈 (3개 B 반영, 1개 A 유지 + Phase 2 이관 메모)
- Critical gaps: 0
