-- V9: Add identity verification (CCCD) fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS identity_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS citizen_id VARCHAR(20) UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS id_card_front_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS id_card_back_url VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS identity_verified_at TIMESTAMP;

-- Automatically mark existing ADMIN users as verified
UPDATE users SET identity_verified = TRUE WHERE role IN ('ADMIN', 'ROLE_ADMIN');
