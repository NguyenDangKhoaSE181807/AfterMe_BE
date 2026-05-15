-- Fix invalid sample customer password hashes from V6
-- Password for all users below: 123123123

UPDATE users
SET password_hash = '$2a$10$GZaTlTOmLxYycabVKkCz/.kkRQni.ISlKegjb/sehalnYOVfuU1PO'
WHERE email IN (
    'customer1@afterme.com',
    'customer2@afterme.com',
    'customer3@afterme.com',
    'customer4@afterme.com',
    'customer5@afterme.com'
);
