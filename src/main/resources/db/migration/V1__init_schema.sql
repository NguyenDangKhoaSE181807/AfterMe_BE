CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    tone_preference VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL
);

CREATE TABLE trusted_contacts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    relationship VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(255),
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_trusted_contacts_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE habits (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_habits_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE reminders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    habit_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    tone VARCHAR(20) NOT NULL,
    safety_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_reminders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reminders_habit FOREIGN KEY (habit_id) REFERENCES habits (id)
);

CREATE TABLE reminder_schedules (
    id BIGSERIAL PRIMARY KEY,
    reminder_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    interval_value INT,
    days_of_week VARCHAR(100),
    start_datetime TIMESTAMP NOT NULL,
    end_datetime TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_reminder_schedules_reminder FOREIGN KEY (reminder_id) REFERENCES reminders (id)
);

CREATE TABLE reminder_instances (
    id BIGSERIAL PRIMARY KEY,
    reminder_id BIGINT NOT NULL,
    scheduled_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    escalation_level INT NOT NULL DEFAULT 0,
    missed_count INT NOT NULL DEFAULT 0,
    last_notification_at TIMESTAMP NULL,
    resolved_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_reminder_instances_reminder FOREIGN KEY (reminder_id) REFERENCES reminders (id)
);

CREATE TABLE escalation_logs (
    id BIGSERIAL PRIMARY KEY,
    reminder_instance_id BIGINT NOT NULL,
    level INT NOT NULL,
    notification_type VARCHAR(20) NOT NULL,
    triggered_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_escalation_logs_instance FOREIGN KEY (reminder_instance_id) REFERENCES reminder_instances (id)
);

CREATE TABLE user_responses (
    id BIGSERIAL PRIMARY KEY,
    reminder_instance_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    response_time TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_user_responses_instance FOREIGN KEY (reminder_instance_id) REFERENCES reminder_instances (id)
);

CREATE TABLE safety_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    reminder_instance_id BIGINT NOT NULL,
    trusted_contact_id BIGINT NOT NULL,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    triggered_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_safety_events_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_safety_events_instance FOREIGN KEY (reminder_instance_id) REFERENCES reminder_instances (id),
    CONSTRAINT fk_safety_events_trusted_contact FOREIGN KEY (trusted_contact_id) REFERENCES trusted_contacts (id)
);

CREATE INDEX idx_trusted_contacts_user_id ON trusted_contacts (user_id);
CREATE INDEX idx_habits_user_id ON habits (user_id);
CREATE INDEX idx_reminders_user_id ON reminders (user_id);
CREATE INDEX idx_reminders_habit_id ON reminders (habit_id);
CREATE INDEX idx_schedules_reminder_id ON reminder_schedules (reminder_id);
CREATE INDEX idx_instances_reminder_id ON reminder_instances (reminder_id);
CREATE INDEX idx_escalation_logs_instance_id ON escalation_logs (reminder_instance_id);
CREATE INDEX idx_user_responses_instance_id ON user_responses (reminder_instance_id);
CREATE INDEX idx_safety_events_user_id ON safety_events (user_id);
CREATE INDEX idx_safety_events_instance_id ON safety_events (reminder_instance_id);
CREATE INDEX idx_safety_events_contact_id ON safety_events (trusted_contact_id);

-- Insert default admin user
INSERT INTO users (email, password_hash, full_name, tone_preference, status, created_at, deleted_at)
VALUES ('admin@afterme.com', '$2a$10$wH8QwQwQwQwQwQwQwQwOuOQwQwQwQwQwQwQwQwQwQwQwQwQwQwQw', 'Admin', 'NORMAL', 'ACTIVE', NOW(), NULL);
-- Password: 123123123 (bcrypt hash)
