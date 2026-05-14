-- V13__add_schedule_reference_to_reminder_instances.sql
-- Thêm schedule_id cho reminder_instances để quản lý rolling window theo schedule

ALTER TABLE reminder_instances ADD COLUMN schedule_id BIGINT NULL;

ALTER TABLE reminder_instances
    ADD CONSTRAINT fk_reminder_instances_schedule
    FOREIGN KEY (schedule_id) REFERENCES reminder_schedules(id);

CREATE INDEX IF NOT EXISTS idx_reminder_instances_schedule_id ON reminder_instances(schedule_id);
CREATE INDEX IF NOT EXISTS idx_reminder_instances_reminder_schedule_time ON reminder_instances(reminder_id, schedule_id, scheduled_time);