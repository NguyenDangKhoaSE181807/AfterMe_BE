-- V16: Add daily check-in reminder support and user safety state tracking

-- Add source_type column to reminders table
ALTER TABLE reminders
ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Add indexes for source_type
CREATE INDEX idx_reminders_source_type ON reminders (source_type);
CREATE INDEX idx_reminders_user_source_type ON reminders (user_id, source_type);

-- Add response_deadline and next_remind_at columns to reminder_instances table
ALTER TABLE reminder_instances
ADD COLUMN response_deadline TIMESTAMP NULL,
ADD COLUMN next_remind_at TIMESTAMP NULL;

-- Add indexes for the new columns
CREATE INDEX idx_reminder_instances_response_deadline ON reminder_instances (response_deadline);
CREATE INDEX idx_reminder_instances_next_remind_at ON reminder_instances (next_remind_at);

-- Add payload column to user_responses table
ALTER TABLE user_responses
ADD COLUMN payload TEXT NULL;

-- Create user_safety_state table
CREATE TABLE user_safety_state (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    consecutive_missed_count INT NOT NULL DEFAULT 0,
    risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    last_checkin_at TIMESTAMP NULL,
    last_missed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_user_safety_state_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Create indexes for user_safety_state
CREATE INDEX idx_user_safety_state_user_id ON user_safety_state (user_id);
CREATE INDEX idx_user_safety_state_risk_level ON user_safety_state (risk_level);
