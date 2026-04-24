ALTER TABLE email_verification_codes
    ADD COLUMN IF NOT EXISTS purpose VARCHAR(30);

UPDATE email_verification_codes
SET purpose = 'SIGN_UP'
WHERE purpose IS NULL;

ALTER TABLE email_verification_codes
    ALTER COLUMN purpose SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_email_verification_codes_user_purpose
    ON email_verification_codes(user_id, purpose);
