ALTER TABLE user_devices
ADD COLUMN last_latitude NUMERIC(10, 7) NULL,
ADD COLUMN last_longitude NUMERIC(10, 7) NULL,
ADD COLUMN last_location_accuracy_meters INT NULL,
ADD COLUMN last_location_at TIMESTAMP NULL,
ADD COLUMN last_location_source VARCHAR(32) NULL;

CREATE INDEX idx_user_devices_last_location_at
    ON user_devices (user_id, last_location_at);
