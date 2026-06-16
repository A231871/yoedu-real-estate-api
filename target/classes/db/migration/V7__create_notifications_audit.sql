-- ============================================================
-- V7 — Notifications & Audit Log
-- ============================================================

CREATE TABLE notifications (
    id              UUID               PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID               NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(50)        NOT NULL CHECK (type IN ('LISTING_APPROVED', 'LISTING_REJECTED', 'LISTING_EXPIRED', 'SCHEDULE_CREATED', 'SCHEDULE_CONFIRMED', 'SCHEDULE_CANCELLED', 'SCHEDULE_REMINDER', 'SCHEDULE_COMPLETED', 'NEW_MESSAGE', 'NEW_REVIEW', 'REVIEW_REPLY', 'LISTING_PRICE_DROP', 'REPORT_RESOLVED', 'SYSTEM')),
    title           VARCHAR(200)       NOT NULL,
    body            TEXT               NOT NULL,

    reference_type  VARCHAR(50),
    reference_id    UUID,

    is_read         BOOLEAN            NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,

    push_sent       BOOLEAN            NOT NULL DEFAULT FALSE,
    push_sent_at    TIMESTAMPTZ,

    created_at      TIMESTAMPTZ        NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ        NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user   ON notifications(user_id, created_at DESC) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_push   ON notifications(push_sent, created_at)    WHERE push_sent = FALSE;
CREATE INDEX idx_notifications_ref    ON notifications(reference_type, reference_id);
CREATE TRIGGER trg_notifications_updated_at BEFORE UPDATE ON notifications FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE audit_logs (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id     UUID         REFERENCES users(id) ON DELETE SET NULL,
    action       VARCHAR(50)  NOT NULL CHECK (action IN ('LISTING_CREATED', 'LISTING_UPDATED', 'LISTING_STATUS_CHANGED', 'USER_SUSPENDED', 'USER_ROLE_CHANGED', 'REVIEW_HIDDEN', 'REPORT_RESOLVED')),
    entity_type  VARCHAR(50)  NOT NULL,
    entity_id    UUID         NOT NULL,
    old_value    JSONB,
    new_value    JSONB,
    ip_address   INET,
    user_agent   VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_entity  ON audit_logs(entity_type, entity_id, created_at DESC);
CREATE INDEX idx_audit_actor   ON audit_logs(actor_id, created_at DESC);
CREATE INDEX idx_audit_action  ON audit_logs(action, created_at DESC);

CREATE TABLE push_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(500) NOT NULL UNIQUE,
    platform    VARCHAR(20)  NOT NULL CHECK (platform IN ('FCM', 'APNS', 'WEB')),
    device_id   VARCHAR(255),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    last_used   TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_push_tokens_user   ON push_tokens(user_id) WHERE is_active = TRUE;
CREATE TRIGGER trg_push_tokens_updated_at BEFORE UPDATE ON push_tokens FOR EACH ROW EXECUTE FUNCTION set_updated_at();