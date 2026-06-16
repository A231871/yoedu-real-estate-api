-- ============================================================
-- V2 — Users & Authentication
-- ============================================================

CREATE TABLE users (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email             VARCHAR(255) NOT NULL UNIQUE,
    password_hash     VARCHAR(255) NOT NULL,
    full_name         VARCHAR(150) NOT NULL,
    phone             VARCHAR(20),
    avatar_url        VARCHAR(500),
    role              VARCHAR(50)  NOT NULL DEFAULT 'RENTER' CHECK (role IN ('ADMIN', 'AGENT', 'OWNER', 'RENTER')),
    status            VARCHAR(50)  NOT NULL DEFAULT 'PENDING_VERIFY' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'PENDING_VERIFY')),

    email_verified          BOOLEAN      NOT NULL DEFAULT FALSE,
    email_verify_token      VARCHAR(255),
    email_verify_expires_at TIMESTAMPTZ,

    reset_password_token      VARCHAR(255),
    reset_password_expires_at TIMESTAMPTZ,

    bio              TEXT,
    company_name     VARCHAR(200),
    license_number   VARCHAR(100),

    last_login_at    TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email  ON users(email);
CREATE INDEX idx_users_role   ON users(role);
CREATE INDEX idx_users_status ON users(status);

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    device_info VARCHAR(255),
    ip_address  INET,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user    ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token   ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at) WHERE revoked = FALSE;

CREATE TRIGGER trg_refresh_tokens_updated_at BEFORE UPDATE ON refresh_tokens FOR EACH ROW EXECUTE FUNCTION set_updated_at();