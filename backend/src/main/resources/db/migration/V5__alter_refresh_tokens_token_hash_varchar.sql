-- RefreshTokenEntity는 Kotlin String → 기본 VARCHAR 매핑을 사용하므로
-- 컬럼 타입을 CHAR(64) → VARCHAR(64)로 맞춘다.
-- SHA-256 hex 값은 항상 64자라 padding이 없어 데이터 손실은 없다.
ALTER TABLE refresh_tokens
    ALTER COLUMN token_hash TYPE VARCHAR(64);
