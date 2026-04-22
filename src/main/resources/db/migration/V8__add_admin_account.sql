

INSERT INTO users (
    email,
    password_hash,
    full_name,
    tone_preference,
    status,
    role,
    created_at,
    deleted_at
)
VALUES (
    'admin@afterme.com',
    '$2a$10$GZaTlTOmLxYycabVKkCz/.kkRQni.ISlKegjb/sehalnYOVfuU1PO',
    'System Admin',
    'NORMAL',
    'ACTIVE',
    'ADMIN',
    CURRENT_TIMESTAMP,
    NULL
)
ON CONFLICT (email) DO UPDATE
SET
    password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    tone_preference = EXCLUDED.tone_preference,
    status = 'ACTIVE',
    role = 'ADMIN',
    deleted_at = NULL;