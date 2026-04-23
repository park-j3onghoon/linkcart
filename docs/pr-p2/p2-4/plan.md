# PR-P2-4: 사용자별 상품 저장 API

## 목적

Phase 1의 stateless 파싱을 User 소유 영속 리소스로 확장. FE가 Phase 1 `/products/parse` 결과를 저장 요청하면 DB에 User 소유로 persist. 로그인된 사용자는 이전에 저장한 상품 목록을 재로드 가능.

## 엔드포인트

- `POST /api/v1/users/me/products` — 파싱 결과를 User 소유로 저장
- `GET /api/v1/users/me/products` — 내 상품 목록 조회 (최신순)
- `DELETE /api/v1/users/me/products/{id}` — 내 상품 삭제

모두 인증 필수 (Bearer JWT).

## 설계

### 저장 입력 결정

**선택**: 클라이언트가 Phase 1 `parse` 결과(name/price/imageUrl/sourceUrl/mall) 를 그대로 전달.
- 서버가 parse를 재실행하지 않음 → 중복 외부 API 호출 방지.
- 신뢰 범위: 클라이언트가 `parse` 응답을 그대로 보내는지 서버는 검증 안 함 (user가 자기 데이터라 악성 사용의 blast radius 작음).

### DB 스키마 (V3__create_user_products.sql)

```sql
CREATE TABLE user_products (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(500) NOT NULL,
    price_amount BIGINT NOT NULL,
    price_currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
    image_url VARCHAR(1000),
    source_url VARCHAR(2000) NOT NULL,
    mall VARCHAR(32) NOT NULL,
    parser_used VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_products_user_source UNIQUE (user_id, source_url)
);
CREATE INDEX idx_user_products_user_created
    ON user_products(user_id, created_at DESC);
```

- `UNIQUE (user_id, source_url)` — 동일 사용자가 같은 URL 중복 저장 방지.
- `ON DELETE CASCADE` — 사용자 삭제 시 상품도 삭제.
- `idx_user_products_user_created` — 본인 목록 조회 최신순 쿼리 최적화.

### 엔드포인트 상세

**POST /api/v1/users/me/products**
- Request:
```json
{
  "name": "...", "price": { "amount": 1000, "currency": "KRW" },
  "image_url": "...", "source_url": "...",
  "mall": "coupang", "parser_used": "coupang-api"
}
```
- 중복 (user_id + source_url) → 409 CONFLICT
- Response 201: 저장된 UserProduct

**GET /api/v1/users/me/products**
- Response 200: `{ "products": [ ... ] }` (created_at DESC)
- 페이징 Phase 2 스코프 밖 (필요 시 후속 PR)

**DELETE /api/v1/users/me/products/{id}**
- 본인 소유 아니거나 존재 안 함 → 404 (존재 노출 방지로 동일 응답)
- Response 204 No Content

### 권한 / 경로

- `/api/v1/users/me/**` 는 `authenticated()`. JWT의 principal(userId)로 소유자 확인.
- P2-2의 SecurityConfig 는 `anyRequest().authenticated()` 이미 설정 → 신규 경로 자동 보호.

## 파일 변경

**신규**:
- `db/migration/V3__create_user_products.sql`
- `domain/model/UserProduct.kt`
- `domain/port/UserProductRepository.kt`
- `application/user/usecase/SaveUserProductUsecase.kt`
- `application/user/usecase/ListUserProductsUsecase.kt`
- `application/user/usecase/DeleteUserProductUsecase.kt`
- `infrastructure/adapter/persistence/userproduct/` (Entity/JpaRepository/Adapter/Mappers)
- `presentation/api/UserProductController.kt`
- `presentation/dto/SaveUserProductRequest.kt`, `UserProductResponse.kt`, `UserProductsResponse.kt`
- 테스트

**수정**: 없음 (기존 엔드포인트 영향 없음)

예상 diff: ~550줄

## 테스트

- `SaveUserProductUsecaseTest` — 저장 성공 / 중복 409
- `ListUserProductsUsecaseTest` — 본인 목록 정렬
- `DeleteUserProductUsecaseTest` — 본인 소유 삭제 성공 / 타인 소유 → 미삭제 404
- JPA repository는 Integration test에서 (여전히 @Disabled)

## 보안 고려

- 소유자 확인을 Usecase 레이어에서 명시 (`userId == userProduct.userId`).
- DELETE에서 타인 소유 시 404 반환 (존재 정보 노출 방지).
- `source_url` 형식 검증은 `@Pattern` (`https://` 시작)로.
- 가격/이미지 URL 길이 상한으로 DB 레벨 방어.

## 검증

```bash
cd backend && ./gradlew test
```

BUILD SUCCESSFUL, 전체 테스트 통과.
