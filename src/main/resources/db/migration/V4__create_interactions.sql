-- ============================================================
-- V4 — Favorites & Viewing Schedules
-- ============================================================

CREATE TABLE favorites (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    listing_id  UUID         NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    note        VARCHAR(300),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, listing_id)
);
CREATE INDEX idx_favorites_user    ON favorites(user_id, created_at DESC);
CREATE INDEX idx_favorites_listing ON favorites(listing_id);
CREATE TRIGGER trg_favorites_updated_at BEFORE UPDATE ON favorites FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION sync_favorite_count()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE listings SET favorite_count = favorite_count + 1 WHERE id = NEW.listing_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE listings SET favorite_count = GREATEST(favorite_count - 1, 0) WHERE id = OLD.listing_id;
    END IF;
    RETURN NULL;
END;
$$;
CREATE TRIGGER trg_favorites_count AFTER INSERT OR DELETE ON favorites FOR EACH ROW EXECUTE FUNCTION sync_favorite_count();

CREATE TABLE viewing_schedules (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id      UUID            NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    renter_id       UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    agent_id        UUID            REFERENCES users(id) ON DELETE SET NULL,
    scheduled_date  DATE            NOT NULL,
    scheduled_time  TIME            NOT NULL,
    duration_mins   SMALLINT        NOT NULL DEFAULT 30 CHECK (duration_mins BETWEEN 15 AND 180),
    note            TEXT,
    status          VARCHAR(50)     NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
    cancel_reason   TEXT,
    cancelled_by    UUID            REFERENCES users(id) ON DELETE SET NULL,
    cancelled_at    TIMESTAMPTZ,
    confirmed_at    TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    reminder_sent   BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_schedule_slot UNIQUE (listing_id, renter_id, scheduled_date, scheduled_time)
);
CREATE INDEX idx_schedules_listing    ON viewing_schedules(listing_id, scheduled_date);
CREATE INDEX idx_schedules_renter     ON viewing_schedules(renter_id, scheduled_date DESC);
CREATE INDEX idx_schedules_agent      ON viewing_schedules(agent_id) WHERE agent_id IS NOT NULL;
CREATE INDEX idx_schedules_status     ON viewing_schedules(status, scheduled_date);
CREATE INDEX idx_schedules_reminder   ON viewing_schedules(scheduled_date, reminder_sent) WHERE status = 'CONFIRMED' AND reminder_sent = FALSE;
CREATE TRIGGER trg_schedules_updated_at BEFORE UPDATE ON viewing_schedules FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION check_listing_bookable()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM listings WHERE id = NEW.listing_id AND status = 'APPROVED') THEN
        RAISE EXCEPTION 'Listing % is not available for scheduling', NEW.listing_id;
    END IF;
    IF NEW.scheduled_date < CURRENT_DATE THEN
        RAISE EXCEPTION 'Cannot schedule a viewing in the past';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_check_listing_bookable BEFORE INSERT ON viewing_schedules FOR EACH ROW EXECUTE FUNCTION check_listing_bookable();