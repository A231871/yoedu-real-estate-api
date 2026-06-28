-- ============================================================
-- V5 — Messaging
-- ============================================================

CREATE TABLE conversations (
    id               UUID         PRIMARY KEY DEFAULT uuidv7(),
    listing_id       UUID         NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    client_id        UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    host_id          UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    last_message_at      TIMESTAMPTZ,
    last_message_preview VARCHAR(200),

    client_unread_count  INT          NOT NULL DEFAULT 0 CHECK (client_unread_count >= 0),
    host_unread_count    INT          NOT NULL DEFAULT 0 CHECK (host_unread_count >= 0),

    client_deleted_at    TIMESTAMPTZ,
    host_deleted_at      TIMESTAMPTZ,

    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_different_participants CHECK (client_id <> host_id)
);
CREATE UNIQUE INDEX idx_conversation_unique ON conversations(listing_id, client_id, host_id) WHERE client_deleted_at IS NULL AND host_deleted_at IS NULL;

CREATE INDEX idx_conv_client  ON conversations(client_id, last_message_at DESC NULLS LAST) WHERE client_deleted_at IS NULL;
CREATE INDEX idx_conv_host    ON conversations(host_id, last_message_at DESC NULLS LAST) WHERE host_deleted_at IS NULL;
CREATE INDEX idx_conv_listing ON conversations(listing_id);
CREATE TRIGGER trg_conversations_updated_at BEFORE UPDATE ON conversations FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION check_conversation_host()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM listings 
        WHERE id = NEW.listing_id 
        AND (owner_id = NEW.host_id OR agent_id = NEW.host_id)
    ) THEN
        RAISE EXCEPTION 'Host does not own or manage this listing';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_check_conversation_host BEFORE INSERT OR UPDATE OF host_id ON conversations FOR EACH ROW EXECUTE FUNCTION check_conversation_host();

CREATE TABLE messages (
    id              UUID         PRIMARY KEY DEFAULT uuidv7(),
    conversation_id UUID         NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id       UUID         REFERENCES users(id) ON DELETE SET NULL,

    content         TEXT,
    message_type    VARCHAR(50)  NOT NULL DEFAULT 'TEXT' CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE', 'SYSTEM')),

    attachment_url      VARCHAR(500),
    attachment_name     VARCHAR(255),
    attachment_size     INT,
    attachment_mime     VARCHAR(100),

    read_at             TIMESTAMPTZ,
    deleted_by_sender   BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_by_receiver BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    is_system           BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_message_content CHECK (
        (message_type = 'TEXT'   AND content   IS NOT NULL AND trim(content) <> '') OR
        (message_type = 'SYSTEM' AND content   IS NOT NULL AND trim(content) <> '') OR
        (message_type IN ('IMAGE','FILE') AND attachment_url IS NOT NULL)
    ),
    CONSTRAINT chk_system_sender CHECK (
        (is_system = TRUE) OR (sender_id IS NOT NULL)
    )
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at ASC);
CREATE INDEX idx_messages_sender       ON messages(sender_id);
CREATE INDEX idx_messages_unread       ON messages(conversation_id, read_at) WHERE read_at IS NULL AND deleted_at IS NULL;
CREATE TRIGGER trg_messages_updated_at BEFORE UPDATE ON messages FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION verify_message_sender()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    conv conversations%ROWTYPE;
BEGIN
    SELECT * INTO conv FROM conversations WHERE id = NEW.conversation_id;
    IF NEW.sender_id <> conv.client_id AND NEW.sender_id <> conv.host_id AND NEW.is_system = FALSE THEN
        RAISE EXCEPTION 'Sender is not a participant of this conversation';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_verify_message_sender BEFORE INSERT ON messages FOR EACH ROW EXECUTE FUNCTION verify_message_sender();

CREATE OR REPLACE FUNCTION increment_unread_count()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    conv conversations%ROWTYPE;
BEGIN
    SELECT * INTO conv FROM conversations WHERE id = NEW.conversation_id;
    
    IF NEW.sender_id = conv.client_id THEN
        UPDATE conversations 
        SET host_unread_count = host_unread_count + 1, last_message_at = now() 
        WHERE id = NEW.conversation_id;
    ELSIF NEW.sender_id = conv.host_id THEN
        UPDATE conversations 
        SET client_unread_count = client_unread_count + 1, last_message_at = now() 
        WHERE id = NEW.conversation_id;
    END IF;
    
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_increment_unread 
AFTER INSERT ON messages 
FOR EACH ROW EXECUTE FUNCTION increment_unread_count();
