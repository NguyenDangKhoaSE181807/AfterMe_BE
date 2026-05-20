ALTER TABLE digital_assets
    ADD COLUMN updated_at TIMESTAMP NULL;

UPDATE digital_assets
SET updated_at = created_at
WHERE updated_at IS NULL;

CREATE INDEX idx_digital_assets_user_id_deleted_at ON digital_assets (user_id, deleted_at);
CREATE INDEX idx_digital_assets_name ON digital_assets (name);
CREATE INDEX idx_digital_assets_identifier ON digital_assets (identifier);
CREATE INDEX idx_digital_assets_type ON digital_assets (type);
