-- Sample Data Insertion for AfterMe Application
-- Generated: April 2026

-- ============================================
-- PLANS (Subscription Plans)
-- ============================================
INSERT INTO plans (name, price, billing_cycle, max_reminders, max_trusted_contacts, max_digital_assets, features, is_active, created_at)
VALUES 
    ('FREE', 0.00, 'MONTHLY', 5, 3, 2, 'Basic reminders, Limited contacts, Basic digital asset storage', TRUE, NOW()),
    ('PREMIUM', 9.99, 'MONTHLY', 50, 10, 20, 'Advanced reminders, Full contacts, Extended assets, Priority support', TRUE, NOW()),
    ('FAMILY', 19.99, 'MONTHLY', 200, 20, 50, 'Unlimited reminders, Family sharing, Full asset management, 24/7 support', TRUE, NOW());

-- ============================================
-- USERS (Test Users)
-- ============================================
-- Note: admin@afterme.com is already created in V1__init_schema.sql
INSERT INTO users (email, password_hash, full_name, tone_preference, status, role, created_at, deleted_at)
VALUES 
    ('customer1@afterme.com', '$2a$10$wH8QwQwQwQwQwQwQwQwOQwQwQwQwQwQwQwQwQwQwQwQwQwQw', 'John Doe', 'PROFESSIONAL', 'ACTIVE', 'CUSTOMER', NOW(), NULL),
    ('customer2@afterme.com', '$2a$10$wH8QwQwQwQwQwQwQwQwOQwQwQwQwQwQwQwQwQwQwQwQwQwQw', 'Jane Smith', 'CASUAL', 'ACTIVE', 'CUSTOMER', NOW(), NULL),
    ('customer3@afterme.com', '$2a$10$wH8QwQwQwQwQwQwQwQwOQwQwQwQwQwQwQwQwQwQwQwQwQwQw', 'Michael Johnson', 'FORMAL', 'ACTIVE', 'CUSTOMER', NOW(), NULL),
    ('customer4@afterme.com', '$2a$10$wH8QwQwQwQwQwQwQwQwOQwQwQwQwQwQwQwQwQwQwQwQwQwQw', 'Sarah Wilson', 'NORMAL', 'ACTIVE', 'CUSTOMER', NOW(), NULL),
    ('customer5@afterme.com', '$2a$10$wH8QwQwQwQwQwQwQwQwOQwQwQwQwQwQwQwQwQwQwQwQwQwQw', 'Robert Brown', 'PROFESSIONAL', 'INACTIVE', 'CUSTOMER', NOW(), NULL);

-- ============================================
-- USER SUBSCRIPTIONS
-- ============================================
-- Note: User IDs 2-6 are used (admin is ID 1)
INSERT INTO user_subscriptions (user_id, plan_id, status, start_at, end_at, auto_renew, created_at)
VALUES 
    (2, 2, 'ACTIVE', NOW() - INTERVAL '30 days', NOW() + INTERVAL '30 days', TRUE, NOW()),
    (3, 1, 'ACTIVE', NOW() - INTERVAL '60 days', NOW() + INTERVAL '30 days', TRUE, NOW()),
    (4, 3, 'ACTIVE', NOW() - INTERVAL '15 days', NOW() + INTERVAL '45 days', TRUE, NOW()),
    (5, 2, 'ACTIVE', NOW() - INTERVAL '45 days', NOW() + INTERVAL '15 days', TRUE, NOW()),
    (6, 1, 'EXPIRED', NOW() - INTERVAL '90 days', NOW() - INTERVAL '30 days', FALSE, NOW());

-- Update users with current plan info
UPDATE users 
SET current_plan_id = 2, plan_expires_at = NOW() + INTERVAL '30 days'
WHERE id = 2;

UPDATE users 
SET current_plan_id = 1, plan_expires_at = NOW() + INTERVAL '30 days'
WHERE id = 3;

UPDATE users 
SET current_plan_id = 3, plan_expires_at = NOW() + INTERVAL '45 days'
WHERE id = 4;

UPDATE users 
SET current_plan_id = 2, plan_expires_at = NOW() + INTERVAL '15 days'
WHERE id = 5;

-- ============================================
-- SUBSCRIPTION HISTORIES
-- ============================================
INSERT INTO subscription_histories (user_id, from_plan_id, to_plan_id, changed_at)
VALUES 
    (2, 1, 2, NOW() - INTERVAL '30 days'),
    (4, 2, 3, NOW() - INTERVAL '15 days'),
    (5, 1, 2, NOW() - INTERVAL '45 days');

-- ============================================
-- TRANSACTIONS
-- ============================================
INSERT INTO transactions (user_id, subscription_id, amount, currency, payment_method, status, transaction_ref, paid_at, created_at)
VALUES 
    (2, 1, 9.99, 'USD', 'CREDIT_CARD', 'COMPLETED', 'TXN20260421001', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days'),
    (3, 2, 0.00, 'USD', 'FREE_PLAN', 'COMPLETED', 'TXN20260421002', NOW(), NOW()),
    (4, 3, 19.99, 'USD', 'CREDIT_CARD', 'COMPLETED', 'TXN20260421003', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days'),
    (5, 4, 9.99, 'USD', 'PAYPAL', 'COMPLETED', 'TXN20260421004', NOW() - INTERVAL '45 days', NOW() - INTERVAL '45 days');

-- ============================================
-- TRUSTED CONTACTS
-- ============================================
INSERT INTO trusted_contacts (user_id, full_name, relationship, phone, email, is_active, created_at)
VALUES 
    (2, 'Emily Doe', 'SPOUSE', '+1234567890', 'emily.doe@example.com', TRUE, NOW()),
    (2, 'David Doe', 'SON', '+1234567891', 'david.doe@example.com', TRUE, NOW()),
    (2, 'Patricia Doe', 'MOTHER', '+1234567892', 'patricia.doe@example.com', TRUE, NOW()),
    (3, 'Tom Smith', 'BROTHER', '+1234567893', 'tom.smith@example.com', TRUE, NOW()),
    (3, 'Jennifer Smith', 'SISTER', '+1234567894', 'jennifer.smith@example.com', TRUE, NOW()),
    (4, 'Rachel Johnson', 'SPOUSE', '+1234567895', 'rachel.johnson@example.com', TRUE, NOW()),
    (5, 'Mark Wilson', 'BEST_FRIEND', '+1234567896', 'mark.wilson@example.com', TRUE, NOW()),
    (5, 'Linda Wilson', 'MOTHER', '+1234567897', 'linda.wilson@example.com', FALSE, NOW());

-- ============================================
-- HABITS
-- ============================================
INSERT INTO habits (user_id, name, category, created_at)
VALUES 
    (2, 'Morning Exercise', 'HEALTH', NOW()),
    (2, 'Meditation', 'WELLNESS', NOW()),
    (2, 'Read News', 'PERSONAL_GROWTH', NOW()),
    (3, 'Take Medications', 'HEALTH', NOW()),
    (3, 'Yoga Session', 'WELLNESS', NOW()),
    (4, 'Code Review', 'WORK', NOW()),
    (4, 'Team Standup', 'WORK', NOW()),
    (5, 'Journaling', 'WELLNESS', NOW()),
    (5, 'Family Time', 'FAMILY', NOW());

-- ============================================
-- REMINDERS
-- ============================================
INSERT INTO reminders (user_id, habit_id, title, description, tone, safety_enabled, status, created_at)
VALUES 
    (2, 1, 'Morning Jog', 'Time to go jogging in the park', 'PROFESSIONAL', TRUE, 'ACTIVE', NOW()),
    (2, 2, 'Meditation Time', '20 minutes meditation session', 'CASUAL', FALSE, 'ACTIVE', NOW()),
    (2, 3, 'Read News', 'Check latest news articles', 'NORMAL', FALSE, 'ACTIVE', NOW()),
    (3, 4, 'Take Blood Pressure Meds', 'Take your prescribed medications', 'FORMAL', TRUE, 'ACTIVE', NOW()),
    (3, 5, 'Evening Yoga', 'Relaxing yoga session before bed', 'CASUAL', FALSE, 'ACTIVE', NOW()),
    (4, 6, 'Code Review', 'Review pull requests from team', 'PROFESSIONAL', FALSE, 'ACTIVE', NOW()),
    (4, 7, 'Team Standup', 'Attend daily standup meeting', 'FORMAL', FALSE, 'PAUSED', NOW()),
    (5, 8, 'Journal Reflection', 'Write down your thoughts and feelings', 'CASUAL', FALSE, 'ACTIVE', NOW()),
    (5, 9, 'Family Dinner', 'Have dinner with family', 'NORMAL', FALSE, 'ACTIVE', NOW());

-- ============================================
-- REMINDER SCHEDULES
-- ============================================
INSERT INTO reminder_schedules (reminder_id, type, interval_value, days_of_week, start_datetime, end_datetime)
VALUES 
    (1, 'DAILY', NULL, NULL, NOW(), NULL),
    (2, 'DAILY', NULL, NULL, NOW(), NULL),
    (3, 'WEEKLY', NULL, 'MON,WED,FRI', NOW(), NULL),
    (4, 'DAILY', NULL, NULL, NOW(), NULL),
    (5, 'DAILY', NULL, NULL, NOW(), NULL),
    (6, 'DAILY', NULL, 'MON,TUE,WED,THU,FRI', NOW(), NULL),
    (7, 'DAILY', NULL, 'MON,TUE,WED,THU,FRI', NOW(), NULL),
    (8, 'DAILY', NULL, NULL, NOW(), NULL),
    (9, 'WEEKLY', NULL, 'FRI,SAT,SUN', NOW(), NULL);

-- ============================================
-- REMINDER INSTANCES
-- ============================================
INSERT INTO reminder_instances (reminder_id, scheduled_time, status, escalation_level, missed_count, last_notification_at)
VALUES 
    (1, NOW() + INTERVAL '1 day', 'PENDING', 0, 0, NULL),
    (1, NOW() + INTERVAL '2 days', 'PENDING', 0, 0, NULL),
    (2, NOW() + INTERVAL '1 day', 'COMPLETED', 0, 0, NOW()),
    (3, NOW() + INTERVAL '2 days', 'PENDING', 1, 1, NOW() - INTERVAL '1 hour'),
    (4, NOW() + INTERVAL '1 day', 'PENDING', 0, 0, NULL),
    (5, NOW() + INTERVAL '1 day', 'PENDING', 0, 0, NULL),
    (6, NOW() + INTERVAL '1 day', 'PENDING', 0, 0, NULL),
    (7, NOW() + INTERVAL '1 day', 'MISSED', 2, 2, NOW() - INTERVAL '2 hours'),
    (8, NOW() + INTERVAL '1 day', 'PENDING', 0, 0, NULL),
    (9, NOW() + INTERVAL '3 days', 'PENDING', 0, 0, NULL);

-- ============================================
-- ESCALATION LOGS
-- ============================================
INSERT INTO escalation_logs (reminder_instance_id, level, notification_type, triggered_at)
VALUES 
    (3, 1, 'EMAIL', NOW() - INTERVAL '1 hour'),
    (7, 1, 'PUSH_NOTIFICATION', NOW() - INTERVAL '2 hours'),
    (7, 2, 'SMS', NOW() - INTERVAL '1 hour');

-- ============================================
-- USER RESPONSES
-- ============================================
INSERT INTO user_responses (reminder_instance_id, action, response_time)
VALUES 
    (2, 'COMPLETED', NOW()),
    (3, 'SNOOZE', NOW() - INTERVAL '45 minutes'),
    (7, 'DISMISSED', NOW() - INTERVAL '1 hour');

-- ============================================
-- SAFETY EVENTS
-- ============================================
INSERT INTO safety_events (user_id, reminder_instance_id, trusted_contact_id, method, status, triggered_at)
VALUES 
    (3, 7, 4, 'SMS', 'SENT', NOW() - INTERVAL '1 hour'),
    (3, 7, 5, 'EMAIL', 'DELIVERED', NOW() - INTERVAL '55 minutes');

-- ============================================
-- DIGITAL ASSETS
-- ============================================
INSERT INTO digital_assets (user_id, name, type, identifier, encrypted_secret, encryption_iv, encryption_algo, identifier_type, identifier_value, version, access_instructions, is_active, created_at)
VALUES 
    (2, 'Bank Account Credentials', 'BANK_ACCOUNT', 'user123', 'encrypted_password_hash_1', 'iv_1234567890ab', 'AES-256-GCM', 'USERNAME', 'john.doe.bank', 1, 'Access online banking portal', TRUE, NOW()),
    (2, 'Email Password', 'EMAIL_PASSWORD', 'john.doe@gmail.com', 'encrypted_password_hash_2', 'iv_abcdef1234567', 'AES-256-GCM', 'EMAIL', 'john.doe@gmail.com', 1, 'Use in Gmail login', TRUE, NOW()),
    (3, 'Insurance Documents', 'DOCUMENT', 'insurance_ref_123', 'encrypted_document_path', 'iv_xyz9876543210', 'AES-256-GCM', 'REFERENCE', 'INS-2024-001', 1, 'Located in safe deposit box', TRUE, NOW()),
    (4, 'Cryptocurrency Wallet', 'CRYPTO_WALLET', 'wallet_addr_xyz', 'encrypted_private_key', 'iv_zyxwvutsrqpon', 'AES-256-GCM', 'WALLET_ADDRESS', 'bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh', 1, 'Cold storage wallet', TRUE, NOW()),
    (5, 'Social Media Passwords', 'PASSWORD_MANAGER', 'sm_vault_001', 'encrypted_vault_content', 'iv_mnopqrstuvwxyz', 'AES-256-GCM', 'VAULT_ID', 'VAULT-SM-001', 1, 'Access via password manager', TRUE, NOW());

-- ============================================
-- DIGITAL ASSET VERSIONS
-- ============================================
INSERT INTO digital_asset_versions (asset_id, encrypted_secret, encryption_iv, encryption_algo, encryption_key_id, version, created_at, created_by)
VALUES 
    (1, 'encrypted_password_hash_1', 'iv_1234567890ab', 'AES-256-GCM', 'key_v1_john_doe', 1, NOW(), 'customer1@afterme.com'),
    (2, 'encrypted_password_hash_2', 'iv_abcdef1234567', 'AES-256-GCM', 'key_v1_john_doe', 1, NOW(), 'customer1@afterme.com'),
    (3, 'encrypted_document_path', 'iv_xyz9876543210', 'AES-256-GCM', 'key_v1_jane_smith', 1, NOW(), 'customer2@afterme.com'),
    (4, 'encrypted_private_key', 'iv_zyxwvutsrqpon', 'AES-256-GCM', 'key_v1_michael', 1, NOW(), 'customer3@afterme.com'),
    (5, 'encrypted_vault_content', 'iv_mnopqrstuvwxyz', 'AES-256-GCM', 'key_v1_sarah', 1, NOW(), 'customer4@afterme.com');

-- ============================================
-- ASSET SHARES
-- ============================================
INSERT INTO asset_shares (digital_asset_id, trusted_contact_id, unlock_condition, is_unlocked, unlock_status, unlock_delay_hours, created_at)
VALUES 
    (1, 1, 'AFTER_DEATH', FALSE, 'LOCKED', 24, NOW()),
    (1, 3, 'AFTER_DEATH', FALSE, 'LOCKED', 48, NOW()),
    (2, 1, 'AFTER_DEATH', FALSE, 'LOCKED', 24, NOW()),
    (3, 4, 'AFTER_DEATH', FALSE, 'LOCKED', 72, NOW()),
    (3, 5, 'AFTER_DEATH', FALSE, 'LOCKED', 72, NOW()),
    (4, 6, 'AFTER_DEATH', FALSE, 'LOCKED', 24, NOW()),
    (5, 7, 'AFTER_DEATH', FALSE, 'LOCKED', 48, NOW()),
    (5, 8, 'AFTER_DEATH', FALSE, 'LOCKED', 48, NOW());

-- ============================================
-- ASSET ACCESS LOGS
-- ============================================
INSERT INTO asset_access_logs (digital_asset_id, accessed_by, action, reason_code, ip_address, user_agent, request_path, http_method, created_at)
VALUES 
    (1, 'customer1@afterme.com', 'VIEW', 'USER_REQUEST', '192.168.1.100', 'Mozilla/5.0', '/api/assets/1', 'GET', NOW() - INTERVAL '1 day'),
    (1, 'emily.doe@example.com', 'UNLOCK', 'EMERGENCY_ACCESS', '203.0.113.45', 'Mozilla/5.0', '/api/assets/1/unlock', 'POST', NOW() - INTERVAL '12 hours'),
    (2, 'customer1@afterme.com', 'UPDATE', 'USER_REQUEST', '192.168.1.100', 'Mozilla/5.0', '/api/assets/2', 'PUT', NOW() - INTERVAL '2 days'),
    (3, 'customer2@afterme.com', 'VIEW', 'USER_REQUEST', '198.51.100.50', 'Chrome/90', '/api/assets/3', 'GET', NOW() - INTERVAL '3 days'),
    (4, 'customer3@afterme.com', 'VIEW', 'USER_REQUEST', '192.0.2.200', 'Safari/535', '/api/assets/4', 'GET', NOW() - INTERVAL '5 days');

-- ============================================
-- ASSET ACCESS FORENSIC LOGS
-- ============================================
INSERT INTO asset_access_forensic_logs (attempted_asset_id, actor_id, action, reason_code, ip_address, user_agent, request_path, http_method, created_at)
VALUES 
    (1, 'customer1@afterme.com', 'VIEWED', 'AUTHORIZED_ACCESS', '192.168.1.100', 'Mozilla/5.0', '/api/assets/1', 'GET', NOW() - INTERVAL '1 day'),
    (2, 'emily.doe@example.com', 'ATTEMPTED_UNLOCK', 'EMERGENCY_UNLOCK_REQUEST', '203.0.113.45', 'Mozilla/5.0', '/api/assets/2/unlock', 'POST', NOW() - INTERVAL '12 hours'),
    (3, 'unknown.user', 'ATTEMPTED_VIEW', 'UNAUTHORIZED_ACCESS_ATTEMPT', '198.51.100.99', 'UnknownBot/1.0', '/api/assets/3', 'GET', NOW() - INTERVAL '8 hours'),
    (4, 'customer3@afterme.com', 'VIEWED', 'AUTHORIZED_ACCESS', '192.0.2.200', 'Safari/535', '/api/assets/4', 'GET', NOW() - INTERVAL '5 days'),
    (NULL, 'admin@afterme.com', 'SYSTEM_AUDIT', 'ADMIN_AUDIT', '192.168.1.1', 'AdminTool/1.0', '/admin/audit/logs', 'GET', NOW() - INTERVAL '2 hours');

-- ============================================
-- FAMILY MEMBERS
-- ============================================
-- Family subscription (subscription_id 3 is for user 4, michael.johnson with FAMILY plan)
INSERT INTO family_members (subscription_id, user_id, role, created_at)
VALUES 
    (3, 4, 'OWNER', NOW()),
    (3, 5, 'MEMBER', NOW());

-- Commit summary
-- ============================================
-- Total records inserted:
-- - Plans: 3
-- - Users: 5 (customer1..customer5, admin@afterme.com already exists in V1)
-- - User Subscriptions: 5
-- - Subscription Histories: 3
-- - Transactions: 4
-- - Trusted Contacts: 8
-- - Habits: 9
-- - Reminders: 9
-- - Reminder Schedules: 9
-- - Reminder Instances: 10
-- - Escalation Logs: 3
-- - User Responses: 3
-- - Safety Events: 2
-- - Digital Assets: 5
-- - Digital Asset Versions: 5
-- - Asset Shares: 8
-- - Asset Access Logs: 5
-- - Asset Access Forensic Logs: 5
-- - Family Members: 2
