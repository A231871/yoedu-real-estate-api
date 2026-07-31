-- ============================================================
-- V20260730171200 - Create user_profile table
-- ============================================================
-- This migration splits profile-specific fields from the users table
-- into a separate user_profile table to match the JPA entity model

CREATE TABLE user_profile (
    id          UUID         PRIMARY KEY DEFAULT uuidv7(),
    user_id     UUID         NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    full_name   VARCHAR(150) NOT NULL,
    phone       VARCHAR(20),
    avatar_url  VARCHAR(500),
    bio         TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT fk_user_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_profile_user_id ON user_profile(user_id);
CREATE TRIGGER trg_user_profile_updated_at BEFORE UPDATE ON user_profile FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Migrate existing data from users table to user_profile table
INSERT INTO user_profile (user_id, full_name, phone, avatar_url, bio, created_at, updated_at, deleted_at)
SELECT id, full_name, phone, avatar_url, bio, created_at, updated_at, deleted_at
FROM users;

-- Drop the profile-related columns from users table
ALTER TABLE users 
    DROP COLUMN full_name,
    DROP COLUMN phone,
    DROP COLUMN avatar_url,
    DROP COLUMN bio;
