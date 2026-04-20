ALTER TABLE asset_access_logs
    ADD COLUMN reason_code VARCHAR(50) NULL,
    ADD COLUMN request_id VARCHAR(100) NULL,
    ADD COLUMN user_agent VARCHAR(255) NULL,
    ADD COLUMN request_path VARCHAR(255) NULL,
    ADD COLUMN http_method VARCHAR(10) NULL;

CREATE TABLE asset_access_forensic_logs (
    id BIGSERIAL PRIMARY KEY,
    attempted_asset_id BIGINT NULL,
    actor_id VARCHAR(100) NULL,
    action VARCHAR(30) NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    ip_address VARCHAR(64) NULL,
    request_id VARCHAR(100) NULL,
    user_agent VARCHAR(255) NULL,
    request_path VARCHAR(255) NULL,
    http_method VARCHAR(10) NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_asset_access_logs_reason_code ON asset_access_logs (reason_code);
CREATE INDEX idx_asset_access_forensic_logs_attempted_asset_id ON asset_access_forensic_logs (attempted_asset_id);
CREATE INDEX idx_asset_access_forensic_logs_reason_code ON asset_access_forensic_logs (reason_code);