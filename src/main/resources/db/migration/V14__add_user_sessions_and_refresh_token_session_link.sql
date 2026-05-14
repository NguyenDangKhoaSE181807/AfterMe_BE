CREATE TABLE user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(64) NOT NULL,
    ip_address VARCHAR(64),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_user_sessions_user_id ON user_sessions (user_id);
CREATE INDEX idx_user_sessions_device_id ON user_sessions (device_id);
CREATE INDEX idx_user_sessions_status_expires ON user_sessions (status, expires_at);

ALTER TABLE refresh_tokens ADD COLUMN session_id BIGINT;
ALTER TABLE refresh_tokens ADD COLUMN replaced_by_token_hash VARCHAR(128);

INSERT INTO user_sessions (user_id, device_id, ip_address, user_agent, created_at, last_used_at, expires_at, status)
SELECT
    rt.user_id,
    CONCAT('legacy-', rt.id),
    NULL,
    'migrated-legacy-session',
    rt.created_at,
    rt.created_at,
    rt.expires_at,
    CASE WHEN rt.revoked_at IS NULL THEN 'ACTIVE' ELSE 'REVOKED' END
FROM refresh_tokens rt;

UPDATE refresh_tokens rt
SET session_id = us.id
FROM user_sessions us
WHERE us.user_id = rt.user_id
  AND us.device_id = CONCAT('legacy-', rt.id);

ALTER TABLE refresh_tokens ALTER COLUMN session_id SET NOT NULL;
ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_tokens_session FOREIGN KEY (session_id) REFERENCES user_sessions (id);

DROP INDEX IF EXISTS idx_refresh_tokens_user_id;
ALTER TABLE refresh_tokens DROP CONSTRAINT IF EXISTS fk_refresh_tokens_user;
ALTER TABLE refresh_tokens DROP COLUMN user_id;

CREATE INDEX idx_refresh_tokens_session_id ON refresh_tokens (session_id);
