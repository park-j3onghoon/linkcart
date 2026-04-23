CREATE TABLE share_lists (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(200),
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_share_lists_owner_created
    ON share_lists(owner_id, created_at DESC);

CREATE TABLE share_list_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    share_list_id BIGINT NOT NULL REFERENCES share_lists(id) ON DELETE CASCADE,
    name VARCHAR(500) NOT NULL,
    price_amount BIGINT NOT NULL,
    price_currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
    image_url VARCHAR(1000),
    source_url VARCHAR(2000) NOT NULL,
    mall VARCHAR(32) NOT NULL
);

CREATE INDEX idx_share_list_items_share_list
    ON share_list_items(share_list_id);
