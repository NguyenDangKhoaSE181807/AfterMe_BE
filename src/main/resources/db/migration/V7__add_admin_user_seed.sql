-- Add additional admin user seed (separate from V6 sample data)
-- Generated: April 2026

INSERT INTO users (email, password_hash, full_name, tone_preference, status, role, created_at, deleted_at)
VALUES
    ('admin2@afterme.com', '$2a$10$wH8QwQwQwQwQwQwQwQwOQwQwQwQwQwQwQwQwQwQwQwQwQwQw', 'System Admin 2', 'NORMAL', 'ACTIVE', 'ADMIN', NOW(), NULL);
