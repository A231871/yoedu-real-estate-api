-- ============================================================
-- V5 — Messaging
-- ============================================================

CREATE TABLE conversations (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id       UUID         NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    renter_id        UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    host_id          UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    last_message_at      TIMESTAMPTZ,
    last_message_preview VARCHAR(200),

    renter_unread_count  INT          NOT NULL DEFAULT 0 CHECK (renter_unread_count >= 0),
    host_unread_count    INT          NOT NULL DEFAULT 0 CHECK (host_unread_count >= 0),

    renter_deleted_at    TIMESTAMPTZ,
    host_deleted_at      TIMESTAMPTZ,

    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_conversation UNIQUE (listing_id, renter_id, host_id),
    CONSTRAINT chk_different_participants CHECK (renter_id <> host_id)
);

CREATE INDEX idx_conv_renter  ON conversations(renter_id, last_message_at DESC NULLS LAST) WHERE renter_deleted_at IS NULL;
CREATE INDEX idx_conv_host    ON conversations(host_id, last_message_at DESC NULLS LAST) WHERE host_deleted_at IS NULL;
CREATE INDEX idx_conv_listing ON conversations(listing_id);
CREATE TRIGGER trg_conversations_updated_at BEFORE UPDATE ON conversations FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE messages (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID         NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id       UUID         NOT NULL REFERENCES users(id) ON DELETE RESTRICT,

    content         TEXT,
    message_type    VARCHAR(50)  NOT NULL DEFAULT 'TEXT' CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE', 'SYSTEM')),

    attachment_url      VARCHAR(500),
    attachment_name     VARCHAR(255),
    attachment_size     INT,
    attachment_mime     VARCHAR(100),

    read_at             TIMESTAMPTZ,
    deleted_by_sender   BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    is_system           BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_message_content CHECK (
        (message_type = 'TEXT'   AND content   IS NOT NULL) OR
        (message_type = 'SYSTEM' AND content   IS NOT NULL) OR
        (message_type IN ('IMAGE','FILE') AND attachment_url IS NOT NULL)
    )
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at ASC);
CREATE INDEX idx_messages_sender       ON messages(sender_id);
CREATE INDEX idx_messages_unread       ON messages(conversation_id, read_at) WHERE read_at IS NULL AND deleted_at IS NULL;
CREATE TRIGGER trg_messages_updated_at BEFORE UPDATE ON messages FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION after_message_insert()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    conv conversations%ROWTYPE;
    preview TEXT;
BEGIN
    SELECT * INTO conv FROM conversations WHERE id = NEW.conversation_id;

    preview := CASE
        WHEN NEW.message_type = 'TEXT'   THEN LEFT(NEW.content, 120)
        WHEN NEW.message_type = 'IMAGE'  THEN '[Hình ảnh]'
        WHEN NEW.message_type = 'FILE'   THEN '[Tệp đính kèm]'
        WHEN NEW.message_type = 'SYSTEM' THEN NEW.content
        ELSE ''
    END;

    UPDATE conversations SET
        last_message_at      = NEW.created_at,
        last_message_preview = preview,
        renter_unread_count  = CASE WHEN NEW.sender_id <> conv.renter_id THEN renter_unread_count + 1 ELSE renter_unread_count END,
        host_unread_count    = CASE WHEN NEW.sender_id <> conv.host_id  THEN host_unread_count + 1 ELSE host_unread_count END,
        updated_at           = now()
    WHERE id = NEW.conversation_id;

    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_after_message_insert AFTER INSERT ON messages FOR EACH ROW EXECUTE FUNCTION after_message_insert();

CREATE OR REPLACE FUNCTION after_message_read()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    conv conversations%ROWTYPE;
BEGIN
    IF OLD.read_at IS NULL AND NEW.read_at IS NOT NULL THEN
        SELECT * INTO conv FROM conversations WHERE id = NEW.conversation_id;
        UPDATE conversations SET
            renter_unread_count = CASE WHEN NEW.sender_id <> conv.renter_id THEN GREATEST(renter_unread_count - 1, 0) ELSE renter_unread_count END,
            host_unread_count  = CASE WHEN NEW.sender_id <> conv.host_id  THEN GREATEST(host_unread_count  - 1, 0) ELSE host_unread_count END
        WHERE id = NEW.conversation_id;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_after_message_read AFTER UPDATE OF read_at ON messages FOR EACH ROW EXECUTE FUNCTION after_message_read();