CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    type VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    reminder_id BIGINT NULL,
    schedule_id BIGINT NULL,
    instance_id BIGINT NULL,
    metadata TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL
);

CREATE INDEX idx_activity_logs_user_created_at
    ON activity_logs (user_id, created_at DESC)
    WHERE deleted_at IS NULL;
