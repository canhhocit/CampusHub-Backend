-- Migration V8: Add date_of_birth column to users table for age verification (13+ requirement)
ALTER TABLE users ADD COLUMN IF NOT EXISTS date_of_birth DATE;
