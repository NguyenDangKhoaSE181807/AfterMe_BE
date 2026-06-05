ALTER TABLE users
ADD COLUMN daily_check_in_time TIME NOT NULL DEFAULT '20:00:00';

ALTER TABLE trusted_contacts
ADD COLUMN priority INT NOT NULL DEFAULT 1;

CREATE INDEX idx_trusted_contacts_user_priority
    ON trusted_contacts (user_id, priority, created_at);
