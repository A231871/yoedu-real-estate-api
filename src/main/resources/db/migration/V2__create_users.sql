-- ============================================================
-- V2 — Users & Authentication
-- ============================================================

CREATE TABLE users (
    id                UUID         PRIMARY KEY DEFAULT uuidv7(),
    email             VARCHAR(255) NOT NULL,
    password_hash     VARCHAR(255),
    full_name         VARCHAR(150) NOT NULL,
    phone             VARCHAR(20),
    avatar_url        VARCHAR(500),
    auth_provider     VARCHAR(50)  NOT NULL DEFAULT 'LOCAL' CHECK (auth_provider IN ('LOCAL', 'GOOGLE', 'APPLE', 'FACEBOOK')),
    provider_id       VARCHAR(255),
    roles             VARCHAR(50)[] NOT NULL DEFAULT '{RENTER}' CONSTRAINT chk_valid_roles CHECK (roles <@ ARRAY['RENTER', 'OWNER', 'AGENT', 'ADMIN', 'ROLE_RENTER', 'ROLE_OWNER', 'ROLE_AGENT', 'ROLE_ADMIN']::varchar[] AND coalesce(array_length(roles, 1), 0) > 0),
    status            VARCHAR(50)  NOT NULL DEFAULT 'PENDING_VERIFY' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'PENDING_VERIFY')),

    email_verified    BOOLEAN      NOT NULL DEFAULT FALSE,

    bio               TEXT,
    deleted_at        TIMESTAMPTZ,

    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_auth_method CHECK (
        (auth_provider = 'LOCAL' AND password_hash IS NOT NULL) OR 
        (auth_provider != 'LOCAL' AND provider_id IS NOT NULL AND password_hash IS NULL)
    )
);

CREATE UNIQUE INDEX idx_users_email_unique ON users(LOWER(email)) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_email  ON users(LOWER(email));
CREATE UNIQUE INDEX idx_users_provider ON users(auth_provider, provider_id) WHERE auth_provider != 'LOCAL' AND deleted_at IS NULL;
CREATE INDEX idx_users_roles  ON users USING GIN(roles);
CREATE INDEX idx_users_status ON users(status);

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION revoke_tokens_on_suspend()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.status = 'SUSPENDED' AND OLD.status != 'SUSPENDED') OR (NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL) THEN
        UPDATE refresh_tokens SET revoked = TRUE WHERE user_id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_revoke_tokens_on_suspend AFTER UPDATE OF status, deleted_at ON users FOR EACH ROW EXECUTE FUNCTION revoke_tokens_on_suspend();
CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT uuidv7(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(512) NOT NULL UNIQUE,
    device_info VARCHAR(500),
    ip_address  INET,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user    ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token   ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at) WHERE revoked = FALSE;

CREATE TRIGGER trg_refresh_tokens_updated_at BEFORE UPDATE ON refresh_tokens FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE user_auth_tokens (
    id          UUID         PRIMARY KEY DEFAULT uuidv7(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_type  VARCHAR(50)  NOT NULL CHECK (token_type IN ('EMAIL_VERIFY', 'PASSWORD_RESET')),
    token       VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    is_used     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_auth_tokens_user ON user_auth_tokens(user_id);
CREATE UNIQUE INDEX idx_user_auth_tokens_token_unique ON user_auth_tokens(token);

CREATE TABLE agent_profiles (
    user_id          UUID         PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    company_name         VARCHAR(200),
    status           VARCHAR(50)  NOT NULL DEFAULT 'PENDING_VERIFICATION' CHECK (status IN ('PENDING_VERIFICATION', 'APPROVED', 'REJECTED')),
    license_number       VARCHAR(100),
    identity_front_url   VARCHAR(500),
    identity_back_url    VARCHAR(500),
    license_document_url VARCHAR(500),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ
);

CREATE TRIGGER trg_agent_profiles_updated_at BEFORE UPDATE ON agent_profiles FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION check_agent_profile_role()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE id = NEW.user_id AND 'AGENT' = ANY(roles)) THEN
        RAISE EXCEPTION 'User does not have AGENT role';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_check_agent_profile_role BEFORE INSERT OR UPDATE ON agent_profiles FOR EACH ROW EXECUTE FUNCTION check_agent_profile_role();
CREATE TABLE owner_profiles (
    user_id          UUID         PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    identity_front_url   VARCHAR(500) NOT NULL,
    status           VARCHAR(50)  NOT NULL DEFAULT 'PENDING_VERIFICATION' CHECK (status IN ('PENDING_VERIFICATION', 'APPROVED', 'REJECTED')),
    identity_back_url    VARCHAR(500) NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ
);

CREATE TRIGGER trg_owner_profiles_updated_at BEFORE UPDATE ON owner_profiles FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE listing_packages (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price_vnd NUMERIC(18, 0) NOT NULL,
    duration_days INT NOT NULL,
    priority_level INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_listing_packages_updated_at BEFORE UPDATE ON listing_packages FOR EACH ROW EXECUTE FUNCTION set_updated_at();

