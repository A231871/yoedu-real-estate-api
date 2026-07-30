CREATE TABLE users_aud (
    id UUID NOT NULL,
    rev INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT NOT NULL,
    email VARCHAR(255),
    password_hash VARCHAR(255),
    auth_provider VARCHAR(50),
    provider_id VARCHAR(255),
    user_role VARCHAR(255),
    status VARCHAR(255),
    email_verified BOOLEAN,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);

-- V__add_user_profile_aud.sql

CREATE TABLE user_profile_aud (
    id          UUID NOT NULL,
    rev         INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype     SMALLINT NOT NULL,
    user_id     UUID,
    full_name   VARCHAR(150),
    phone       VARCHAR(20),
    avatar_url  VARCHAR(500),
    bio         TEXT,
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    deleted_at  TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
