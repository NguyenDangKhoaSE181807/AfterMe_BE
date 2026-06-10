-- Translate sample/display data from the original seed files to Vietnamese.
-- Keep enum-like/internal values such as FREE, PREMIUM, FAMILY, ACTIVE, CUSTOMER,
-- BANK_ACCOUNT, and FAMILY_MEMBER roles unchanged because application logic uses them.

-- ============================================
-- PLANS
-- ============================================
UPDATE plans
SET features = 'Nhắc nhở cơ bản, Giới hạn người liên hệ tin cậy, Lưu trữ tài sản số cơ bản'
WHERE name = 'FREE';

UPDATE plans
SET features = 'Nhắc nhở nâng cao, Đầy đủ người liên hệ tin cậy, Mở rộng tài sản số, Hỗ trợ ưu tiên'
WHERE name = 'PREMIUM';

UPDATE plans
SET features = 'Nhắc nhở không giới hạn, Chia sẻ gia đình, Quản lý đầy đủ tài sản số, Hỗ trợ 24/7'
WHERE name = 'FAMILY';

-- ============================================
-- USERS
-- ============================================
UPDATE users SET full_name = 'Quản trị viên hệ thống' WHERE email = 'admin@afterme.com';
UPDATE users SET full_name = 'Quản trị viên 2' WHERE email = 'admin2@afterme.com';
UPDATE users SET full_name = 'Nguyễn Văn An' WHERE email = 'customer1@afterme.com';
UPDATE users SET full_name = 'Trần Thị Bình' WHERE email = 'customer2@afterme.com';
UPDATE users SET full_name = 'Lê Minh Cường' WHERE email = 'customer3@afterme.com';
UPDATE users SET full_name = 'Phạm Thu Hà' WHERE email = 'customer4@afterme.com';
UPDATE users SET full_name = 'Hoàng Gia Bảo' WHERE email = 'customer5@afterme.com';

UPDATE users SET full_name = 'Quản trị viên hệ thống' WHERE email = 'admin@reminder.local';
UPDATE users SET full_name = 'Khách hàng mẫu' WHERE email = 'customer@reminder.local';
UPDATE users SET full_name = 'Tư vấn viên mẫu' WHERE email = 'consultant@reminder.local';

-- ============================================
-- TRUSTED CONTACTS
-- ============================================
UPDATE trusted_contacts
SET full_name = 'Nguyễn Thị Mai', email = 'mai.nguyen@example.com'
WHERE user_id = 2 AND full_name = 'Emily Doe';

UPDATE trusted_contacts
SET full_name = 'Nguyễn Minh Đức', email = 'duc.nguyen@example.com'
WHERE user_id = 2 AND full_name = 'David Doe';

UPDATE trusted_contacts
SET full_name = 'Trần Thị Lan', email = 'lan.tran@example.com'
WHERE user_id = 2 AND full_name = 'Patricia Doe';

UPDATE trusted_contacts
SET full_name = 'Trần Văn Nam', email = 'nam.tran@example.com'
WHERE user_id = 3 AND full_name = 'Tom Smith';

UPDATE trusted_contacts
SET full_name = 'Trần Ngọc Anh', email = 'anh.tran@example.com'
WHERE user_id = 3 AND full_name = 'Jennifer Smith';

UPDATE trusted_contacts
SET full_name = 'Lê Thu Hương', email = 'huong.le@example.com'
WHERE user_id = 4 AND full_name = 'Rachel Johnson';

UPDATE trusted_contacts
SET full_name = 'Phạm Quốc Huy', email = 'huy.pham@example.com'
WHERE user_id = 5 AND full_name = 'Mark Wilson';

UPDATE trusted_contacts
SET full_name = 'Hoàng Thị Nga', email = 'nga.hoang@example.com'
WHERE user_id = 5 AND full_name = 'Linda Wilson';

-- ============================================
-- HABITS
-- ============================================
UPDATE habits SET name = 'Tập thể dục buổi sáng' WHERE user_id = 2 AND name = 'Morning Exercise';
UPDATE habits SET name = 'Thiền 20 phút' WHERE user_id = 2 AND name = 'Meditation';
UPDATE habits SET name = 'Đọc tin tức' WHERE user_id = 2 AND name = 'Read News';
UPDATE habits SET name = 'Uống thuốc huyết áp' WHERE user_id = 3 AND name = 'Take Medications';
UPDATE habits SET name = 'Tập yoga buổi tối' WHERE user_id = 3 AND name = 'Yoga Session';
UPDATE habits SET name = 'Review code' WHERE user_id = 4 AND name = 'Code Review';
UPDATE habits SET name = 'Họp daily với team' WHERE user_id = 4 AND name = 'Team Standup';
UPDATE habits SET name = 'Viết nhật ký' WHERE user_id = 5 AND name = 'Journaling';
UPDATE habits SET name = 'Thời gian cho gia đình' WHERE user_id = 5 AND name = 'Family Time';

-- ============================================
-- REMINDERS
-- ============================================
UPDATE reminders
SET title = 'Chạy bộ buổi sáng',
    description = 'Đến giờ ra công viên chạy bộ.'
WHERE user_id = 2 AND title = 'Morning Jog';

UPDATE reminders
SET title = 'Đến giờ thiền',
    description = 'Thiền 20 phút để thư giãn đầu óc.'
WHERE user_id = 2 AND title = 'Meditation Time';

UPDATE reminders
SET title = 'Đọc tin mới',
    description = 'Kiểm tra các tin tức mới nhất trong ngày.'
WHERE user_id = 2 AND title = 'Read News';

UPDATE reminders
SET title = 'Uống thuốc huyết áp',
    description = 'Uống đúng liều thuốc đã được bác sĩ kê.'
WHERE user_id = 3 AND title = 'Take Blood Pressure Meds';

UPDATE reminders
SET title = 'Yoga buổi tối',
    description = 'Tập yoga nhẹ trước khi ngủ.'
WHERE user_id = 3 AND title = 'Evening Yoga';

UPDATE reminders
SET title = 'Review code',
    description = 'Kiểm tra pull request của team.'
WHERE user_id = 4 AND title = 'Code Review';

UPDATE reminders
SET title = 'Họp daily',
    description = 'Tham gia cuộc họp daily của team.'
WHERE user_id = 4 AND title = 'Team Standup';

UPDATE reminders
SET title = 'Viết nhật ký cảm xúc',
    description = 'Ghi lại suy nghĩ và cảm xúc trong ngày.'
WHERE user_id = 5 AND title = 'Journal Reflection';

UPDATE reminders
SET title = 'Bữa tối gia đình',
    description = 'Ăn tối và trò chuyện với gia đình.'
WHERE user_id = 5 AND title = 'Family Dinner';

-- ============================================
-- DIGITAL ASSETS
-- ============================================
UPDATE digital_assets
SET name = 'Thông tin đăng nhập ngân hàng',
    identifier_value = 'nguyen.van.an.bank',
    access_instructions = 'Truy cập cổng ngân hàng trực tuyến.'
WHERE user_id = 2 AND name = 'Bank Account Credentials';

UPDATE digital_assets
SET name = 'Mật khẩu email cá nhân',
    identifier = 'an.nguyen@gmail.com',
    identifier_value = 'an.nguyen@gmail.com',
    access_instructions = 'Dùng khi đăng nhập Gmail.'
WHERE user_id = 2 AND name = 'Email Password';

UPDATE digital_assets
SET name = 'Hồ sơ bảo hiểm',
    identifier = 'bao_hiem_123',
    identifier_value = 'BH-2024-001',
    access_instructions = 'Cất trong két sắt gia đình.'
WHERE user_id = 3 AND name = 'Insurance Documents';

UPDATE digital_assets
SET name = 'Ví tiền mã hóa',
    access_instructions = 'Ví lạnh lưu trữ ngoại tuyến.'
WHERE user_id = 4 AND name = 'Cryptocurrency Wallet';

UPDATE digital_assets
SET name = 'Kho mật khẩu mạng xã hội',
    identifier_value = 'VAULT-MXH-001',
    access_instructions = 'Truy cập qua trình quản lý mật khẩu.'
WHERE user_id = 5 AND name = 'Social Media Passwords';

-- ============================================
-- DIGITAL ASSET VERSIONS
-- ============================================
UPDATE digital_asset_versions SET encryption_key_id = 'key_v1_nguyen_an' WHERE encryption_key_id = 'key_v1_john_doe';
UPDATE digital_asset_versions SET encryption_key_id = 'key_v1_tran_binh' WHERE encryption_key_id = 'key_v1_jane_smith';
UPDATE digital_asset_versions SET encryption_key_id = 'key_v1_le_cuong' WHERE encryption_key_id = 'key_v1_michael';
UPDATE digital_asset_versions SET encryption_key_id = 'key_v1_pham_ha' WHERE encryption_key_id = 'key_v1_sarah';

-- ============================================
-- ASSET ACCESS LOGS
-- ============================================
UPDATE asset_access_logs
SET accessed_by = 'mai.nguyen@example.com'
WHERE accessed_by = 'emily.doe@example.com';

UPDATE asset_access_forensic_logs
SET actor_id = 'mai.nguyen@example.com'
WHERE actor_id = 'emily.doe@example.com';

UPDATE asset_access_forensic_logs
SET actor_id = 'nguoi.la'
WHERE actor_id = 'unknown.user';
