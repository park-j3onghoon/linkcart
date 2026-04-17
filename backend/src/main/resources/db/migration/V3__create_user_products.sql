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
