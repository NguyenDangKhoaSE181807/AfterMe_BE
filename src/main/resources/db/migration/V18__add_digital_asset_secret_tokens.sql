CREATE TABLE digital_asset_secret_tokens (
    id BIGSERIAL PRIMARY KEY,
    digital_asset_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    actor_id VARCHAR(100) NOT NULL,
    ip_address VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_digital_asset_secret_tokens_asset FOREIGN KEY (digital_asset_id) REFERENCES digital_assets (id)
);

CREATE INDEX idx_digital_asset_secret_tokens_asset_id ON digital_asset_secret_tokens (digital_asset_id);
CREATE INDEX idx_digital_asset_secret_tokens_expires_at ON digital_asset_secret_tokens (expires_at);
