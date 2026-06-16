-- ============================================================
-- V6 — Reviews & Reports
-- ============================================================

CREATE TABLE reviews (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id    UUID         NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    reviewer_id   UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    schedule_id   UUID         REFERENCES viewing_schedules(id) ON DELETE SET NULL,

    rating        SMALLINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    location_rating   SMALLINT CHECK (location_rating BETWEEN 1 AND 5),
    accuracy_rating   SMALLINT CHECK (accuracy_rating BETWEEN 1 AND 5),
    host_rating       SMALLINT CHECK (host_rating      BETWEEN 1 AND 5),

    title         VARCHAR(200),
    comment       TEXT         NOT NULL,

    is_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_hidden     BOOLEAN      NOT NULL DEFAULT FALSE,

    owner_reply       TEXT,
    owner_replied_at  TIMESTAMPTZ,

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_review_per_listing UNIQUE (listing_id, reviewer_id)
);

CREATE INDEX idx_reviews_listing   ON reviews(listing_id, created_at DESC) WHERE is_hidden = FALSE;
CREATE INDEX idx_reviews_reviewer  ON reviews(reviewer_id);
CREATE INDEX idx_reviews_rating    ON reviews(listing_id, rating) WHERE is_hidden = FALSE;
CREATE TRIGGER trg_reviews_updated_at BEFORE UPDATE ON reviews FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION check_review_eligibility()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    -- If schedule_id is not provided, try to auto-detect a completed schedule
    IF NEW.schedule_id IS NULL THEN
        SELECT id INTO NEW.schedule_id FROM viewing_schedules 
        WHERE renter_id = NEW.reviewer_id AND listing_id = NEW.listing_id AND status = 'COMPLETED'
        LIMIT 1;
        
        IF NEW.schedule_id IS NULL THEN
            RAISE EXCEPTION 'A completed viewing schedule is required to submit a review';
        END IF;
    END IF;

    -- Verify that the schedule is completed and belongs to the reviewer and listing
    IF NOT EXISTS (
        SELECT 1 FROM viewing_schedules
        WHERE id = NEW.schedule_id
          AND renter_id = NEW.reviewer_id
          AND listing_id = NEW.listing_id
          AND status = 'COMPLETED'
    ) THEN
        RAISE EXCEPTION 'The provided viewing schedule is not completed or invalid';
    END IF;

    NEW.is_verified := TRUE;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_check_review_eligibility BEFORE INSERT ON reviews FOR EACH ROW EXECUTE FUNCTION check_review_eligibility();

CREATE TABLE reports (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id    UUID          NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    reporter_id   UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    reason        VARCHAR(50)   NOT NULL CHECK (reason IN ('FRAUD', 'DUPLICATE', 'WRONG_INFO', 'INAPPROPRIATE_CONTENT', 'WRONG_PRICE', 'ALREADY_RENTED', 'OTHER')),
    description   TEXT,
    evidence_urls TEXT[],

    status        VARCHAR(50)   NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED')),
    admin_note    TEXT,
    resolved_by   UUID          REFERENCES users(id) ON DELETE SET NULL,
    resolved_at   TIMESTAMPTZ,

    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_report_per_user UNIQUE (listing_id, reporter_id)
);

CREATE INDEX idx_reports_listing  ON reports(listing_id);
CREATE INDEX idx_reports_status   ON reports(status, created_at DESC);
CREATE INDEX idx_reports_reporter ON reports(reporter_id);
CREATE TRIGGER trg_reports_updated_at BEFORE UPDATE ON reports FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION auto_flag_listing_on_reports()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    report_count INT;
BEGIN
    SELECT COUNT(*) INTO report_count FROM reports WHERE listing_id = NEW.listing_id AND status = 'PENDING';
    IF report_count >= 5 THEN
        UPDATE listings SET status = 'PENDING', updated_at = now() WHERE id = NEW.listing_id AND status = 'APPROVED';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_auto_flag_listing AFTER INSERT ON reports FOR EACH ROW EXECUTE FUNCTION auto_flag_listing_on_reports();