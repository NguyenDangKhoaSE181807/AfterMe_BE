CREATE TABLE user_devices (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(64) NOT NULL,
    fcm_token VARCHAR(1024) NULL,
    platform VARCHAR(32) NULL,
    is_trusted BOOLEAN NOT NULL DEFAULT FALSE,
    notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP NULL,
    CONSTRAINT fk_user_devices_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_user_devices_user_device UNIQUE (user_id, device_id)
);

CREATE INDEX idx_user_devices_user_id ON user_devices (user_id);
CREATE INDEX idx_user_devices_device_id ON user_devices (device_id);
CREATE INDEX idx_user_devices_notification_enabled ON user_devices (notification_enabled);