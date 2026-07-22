ALTER TABLE users
    ADD COLUMN IF NOT EXISTS passive_activity_assist_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS user_activity_states (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    last_app_foreground_at TIMESTAMP NULL,
    last_app_interaction_at TIMESTAMP NULL,
    last_push_tapped_at TIMESTAMP NULL,
    last_device_unlocked_at TIMESTAMP NULL,
    last_device_interactive_at TIMESTAMP NULL,
    last_motion_at TIMESTAMP NULL,
    last_activity_type VARCHAR(40) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_activity_states_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_activity_states_user_device
        UNIQUE (user_id, device_id)
);

CREATE INDEX IF NOT EXISTS idx_user_activity_states_user_updated
    ON user_activity_states (user_id, updated_at DESC);
