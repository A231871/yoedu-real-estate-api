-- Add version column for optimistic locking to users table
ALTER TABLE users ADD COLUMN version INT DEFAULT 0 NOT NULL;
