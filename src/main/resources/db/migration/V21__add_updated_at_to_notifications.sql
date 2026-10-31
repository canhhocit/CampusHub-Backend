-- Migration V21: Add updated_at column to notifications table for BaseEntity compatibility
ALTER TABLE notifications
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

