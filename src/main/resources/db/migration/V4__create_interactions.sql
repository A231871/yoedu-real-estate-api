-- ============================================================
-- V4 — Favorites & Viewing Schedules
-- ============================================================

CREATE TABLE favorites (
    id          UUID         PRIMARY KEY DEFAULT uuidv7(),
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




CREATE TABLE viewing_schedules (
    id              UUID            PRIMARY KEY DEFAULT uuidv7(),
    listing_id      UUID            NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    client_id       UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    host_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scheduled_start TIMESTAMPTZ     NOT NULL,
    scheduled_end   TIMESTAMPTZ     NOT NULL,
    duration_mins   INT             GENERATED ALWAYS AS (EXTRACT(EPOCH FROM (scheduled_end - scheduled_start))/60) STORED,
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
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT no_client_double_booking EXCLUDE USING gist (
        client_id WITH =,
        tstzrange(scheduled_start, scheduled_end) WITH &&
    ) WHERE (status IN ('PENDING', 'CONFIRMED') AND deleted_at IS NULL),
    CONSTRAINT no_host_double_booking EXCLUDE USING gist (
        host_id WITH =,
        tstzrange(scheduled_start, scheduled_end) WITH &&
    ) WHERE (status IN ('PENDING', 'CONFIRMED') AND deleted_at IS NULL),
    CONSTRAINT chk_diff_users CHECK (client_id <> host_id),
    CONSTRAINT chk_schedule_dates CHECK (scheduled_end > scheduled_start)
);
CREATE INDEX idx_schedules_listing    ON viewing_schedules(listing_id, scheduled_start);
CREATE INDEX idx_schedules_client     ON viewing_schedules(client_id, scheduled_start DESC);
CREATE INDEX idx_schedules_host       ON viewing_schedules(host_id);
CREATE INDEX idx_schedules_status     ON viewing_schedules(status, scheduled_start);
CREATE INDEX idx_schedules_reminder   ON viewing_schedules(scheduled_start, reminder_sent) WHERE status = 'CONFIRMED' AND reminder_sent = FALSE;
CREATE TRIGGER trg_schedules_updated_at BEFORE UPDATE ON viewing_schedules FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION check_listing_bookable()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF (TG_OP = 'INSERT' OR 
        NEW.scheduled_start != OLD.scheduled_start OR 
        NEW.scheduled_end != OLD.scheduled_end OR
        (TG_OP = 'UPDATE' AND NEW.status IN ('PENDING', 'CONFIRMED') AND OLD.status NOT IN ('PENDING', 'CONFIRMED'))) THEN
        
        IF NOT EXISTS (SELECT 1 FROM listings WHERE id = NEW.listing_id AND status = 'APPROVED' AND deleted_at IS NULL) THEN
            RAISE EXCEPTION 'Listing % is not available for scheduling', NEW.listing_id;
        END IF;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM listings 
        WHERE id = NEW.listing_id 
        AND (owner_id = NEW.host_id OR agent_id = NEW.host_id)
    ) THEN
        RAISE EXCEPTION 'Host does not own or manage this listing';
    END IF;
    IF NEW.scheduled_start < now() AND (TG_OP = 'INSERT' OR NEW.scheduled_start != OLD.scheduled_start) THEN
        RAISE EXCEPTION 'Cannot schedule a viewing in the past';
    END IF;
    IF NEW.status = 'COMPLETED' AND NEW.scheduled_start > now() THEN
        RAISE EXCEPTION 'Cannot complete a viewing schedule in the future';
    END IF;

    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_check_listing_bookable BEFORE INSERT OR UPDATE OF scheduled_start, scheduled_end, status ON viewing_schedules FOR EACH ROW EXECUTE FUNCTION check_listing_bookable();

CREATE OR REPLACE FUNCTION check_contract_owner()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.owner_id != (SELECT owner_id FROM listings WHERE id = NEW.listing_id) THEN
        RAISE EXCEPTION 'Contract owner_id does not match listing owner_id';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TABLE contracts (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    listing_id UUID NOT NULL,
    client_id UUID REFERENCES users(id) ON DELETE SET NULL,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    agent_id UUID REFERENCES users(id) ON DELETE SET NULL,
    schedule_id UUID REFERENCES viewing_schedules(id) ON DELETE SET NULL,
    document_url VARCHAR(500) NOT NULL,
    start_date DATE,
    end_date DATE,
    monthly_rent_agreed NUMERIC(18, 0),
    deposit_agreed NUMERIC(18, 0),
    sale_price_agreed NUMERIC(18, 0),
    notary_date DATE,
    tax_fee_responsibility VARCHAR(50) CHECK (tax_fee_responsibility IN ('BUYER', 'SELLER', 'SHARED', 'RENTER', 'OWNER')),
    listing_type VARCHAR(50) NOT NULL CHECK (listing_type IN ('FOR_RENT', 'FOR_SALE')),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SIGNED_BY_CLIENT', 'SIGNED_BY_OWNER', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_contract_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT chk_contract_type_match CHECK (
        status = 'DRAFT' OR
        (listing_type = 'FOR_RENT' AND monthly_rent_agreed IS NOT NULL AND sale_price_agreed IS NULL) OR 
        (listing_type = 'FOR_SALE' AND sale_price_agreed IS NOT NULL AND monthly_rent_agreed IS NULL)
    ),
    FOREIGN KEY (listing_id, listing_type) REFERENCES listings(id, listing_type) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_different_parties CHECK (client_id <> owner_id)
);
CREATE INDEX idx_contracts_listing ON contracts(listing_id);
CREATE INDEX idx_contracts_client ON contracts(client_id);
CREATE INDEX idx_contracts_owner ON contracts(owner_id);
CREATE TRIGGER trg_check_contract_owner BEFORE INSERT OR UPDATE OF owner_id, listing_id ON contracts FOR EACH ROW EXECUTE FUNCTION check_contract_owner();

CREATE OR REPLACE FUNCTION check_contract_immutable()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status != 'DRAFT' AND (
        NEW.monthly_rent_agreed IS DISTINCT FROM OLD.monthly_rent_agreed OR
        NEW.deposit_agreed IS DISTINCT FROM OLD.deposit_agreed OR
        NEW.sale_price_agreed IS DISTINCT FROM OLD.sale_price_agreed
    ) THEN
        RAISE EXCEPTION 'Cannot modify financial terms of a non-draft contract';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_check_contract_immutable BEFORE UPDATE ON contracts FOR EACH ROW EXECUTE FUNCTION check_contract_immutable();
