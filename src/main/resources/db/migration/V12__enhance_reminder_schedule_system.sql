-- V12__enhance_reminder_schedule_system.sql
-- Migration để hoàn thành luồng nhắc nhở với quản lý lịch chi tiết

-- 1. Thêm cột updated_at vào bảng reminders
ALTER TABLE reminders ADD COLUMN updated_at TIMESTAMP NULL;

-- 2. Thêm cột created_at và updated_at vào bảng reminder_schedules
ALTER TABLE reminder_schedules ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE reminder_schedules ADD COLUMN updated_at TIMESTAMP NULL;

-- 3. Tạo bảng reminder_schedule_days để lưu trữ các ngày trong tuần cho CUSTOM schedules
CREATE TABLE IF NOT EXISTS reminder_schedule_days (
    schedule_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    PRIMARY KEY (schedule_id, day_of_week),
    FOREIGN KEY (schedule_id) REFERENCES reminder_schedules(id) ON DELETE CASCADE
);

-- 4. Tạo index cho bảng reminder_schedules để tối ưu hóa truy vấn
CREATE INDEX IF NOT EXISTS idx_reminder_schedules_reminder_id ON reminder_schedules(reminder_id);
CREATE INDEX IF NOT EXISTS idx_reminder_schedules_deleted_at ON reminder_schedules(deleted_at);

-- 5. Tạo index cho bảng reminders để tối ưu hóa truy vấn theo status
CREATE INDEX IF NOT EXISTS idx_reminders_status ON reminders(status);
CREATE INDEX IF NOT EXISTS idx_reminders_user_id_status ON reminders(user_id, status);

-- 6. Cập nhật dữ liệu hiện có: Nếu schedule không có deleted_at thì status là ACTIVE
-- (Giả định tất cả reminder_schedules hiện tại là active)
UPDATE reminder_schedules SET deleted_at = NULL WHERE deleted_at IS NULL;
