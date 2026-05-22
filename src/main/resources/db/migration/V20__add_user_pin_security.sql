ALTER TABLE users
    ADD COLUMN pin_hash VARCHAR(255) NULL,
    ADD COLUMN pin_failed_attempts INT NULL,
    ADD COLUMN pin_locked_until TIMESTAMP NULL,
    ADD COLUMN pin_updated_at TIMESTAMP NULL;

CREATE INDEX idx_users_pin_locked_until ON users (pin_locked_until);
