ALTER TABLE digital_assets
    ADD COLUMN encryption_key_id VARCHAR(128) NULL,
    ADD COLUMN identifier_type VARCHAR(30) NULL,
    ADD COLUMN identifier_value VARCHAR(255) NULL,
    ADD COLUMN version INT NOT NULL DEFAULT 1;

UPDATE digital_assets
SET identifier_type = 'USERNAME',
    identifier_value = identifier
WHERE identifier_value IS NULL;

ALTER TABLE asset_shares
    ADD COLUMN unlock_status VARCHAR(20) NOT NULL DEFAULT 'LOCKED',
    ADD COLUMN unlocked_by VARCHAR(20) NULL,
    ADD COLUMN unlock_delay_hours INT NULL,
    ADD COLUMN unlock_policy TEXT NULL;

UPDATE asset_shares
SET unlock_status = CASE
    WHEN is_unlocked THEN 'UNLOCKED'
    ELSE 'LOCKED'
END
WHERE unlock_status IS NULL;

CREATE TABLE digital_asset_versions (
    id BIGSERIAL PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    encrypted_secret TEXT NOT NULL,
    encryption_iv VARCHAR(255) NOT NULL,
    encryption_algo VARCHAR(100) NOT NULL,
    encryption_key_id VARCHAR(128) NULL,
    version INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100) NULL,
    CONSTRAINT fk_digital_asset_versions_asset FOREIGN KEY (asset_id) REFERENCES digital_assets (id)
);

CREATE TABLE asset_access_logs (
    id BIGSERIAL PRIMARY KEY,
    digital_asset_id BIGINT NOT NULL,
    accessed_by VARCHAR(100) NOT NULL,
    action VARCHAR(20) NOT NULL,
    ip_address VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_asset_access_logs_asset FOREIGN KEY (digital_asset_id) REFERENCES digital_assets (id)
);

CREATE INDEX idx_digital_assets_identifier_type ON digital_assets (identifier_type);
CREATE INDEX idx_asset_shares_unlock_status ON asset_shares (unlock_status);
CREATE INDEX idx_digital_asset_versions_asset_id ON digital_asset_versions (asset_id);
CREATE INDEX idx_asset_access_logs_asset_id ON asset_access_logs (digital_asset_id);