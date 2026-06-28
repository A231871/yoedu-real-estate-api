-- ============================================================
-- V6 — Reviews & Reports
-- ============================================================

CREATE TABLE reviews (
    id            UUID         PRIMARY KEY DEFAULT uuidv7(),
    listing_id    UUID         NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    reviewer_id   UUID         REFERENCES users(id) ON DELETE SET NULL,
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
    deleted_at    TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_reviews_unique_schedule ON reviews(schedule_id) WHERE schedule_id IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX idx_reviews_one_per_user ON reviews(listing_id, reviewer_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_listing   ON reviews(listing_id, created_at DESC) WHERE is_hidden = FALSE;
CREATE INDEX idx_reviews_reviewer  ON reviews(reviewer_id);
CREATE INDEX idx_reviews_rating    ON reviews(listing_id, rating) WHERE is_hidden = FALSE;
CREATE TRIGGER trg_reviews_updated_at BEFORE UPDATE ON reviews FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION check_review_owner()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.reviewer_id IN (SELECT owner_id FROM listings WHERE id = NEW.listing_id UNION SELECT agent_id FROM listings WHERE id = NEW.listing_id) THEN
        RAISE EXCEPTION 'Owner/Agent cannot review their own listing';
    END IF;
    IF NEW.schedule_id IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM viewing_schedules WHERE id = NEW.schedule_id AND listing_id = NEW.listing_id AND client_id = NEW.reviewer_id AND status = 'COMPLETED') THEN
            RAISE EXCEPTION 'Schedule does not belong to this user/listing or is not completed';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_check_review_owner BEFORE INSERT ON reviews FOR EACH ROW EXECUTE FUNCTION check_review_owner();

CREATE TABLE reports (
    id            UUID          PRIMARY KEY DEFAULT uuidv7(),
    listing_id    UUID          NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    reporter_id   UUID          REFERENCES users(id) ON DELETE SET NULL,

    reason        VARCHAR(50)   NOT NULL CHECK (reason IN ('FRAUD', 'DUPLICATE', 'WRONG_INFO', 'INAPPROPRIATE_CONTENT', 'WRONG_PRICE', 'ALREADY_RENTED', 'OTHER')),
    description   TEXT,
    evidence_urls VARCHAR(500)[],

    status        VARCHAR(50)   NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED')),
    admin_note    TEXT,
    resolved_by   UUID          REFERENCES users(id) ON DELETE SET NULL,
    resolved_at   TIMESTAMPTZ,

    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT chk_evidence_urls_length CHECK (coalesce(array_length(evidence_urls, 1), 0) <= 10)
);

CREATE UNIQUE INDEX uq_report_pending ON reports(listing_id, reporter_id) WHERE status = 'PENDING' AND deleted_at IS NULL;
CREATE INDEX idx_reports_listing  ON reports(listing_id);
CREATE INDEX idx_reports_status   ON reports(status, created_at DESC);
CREATE INDEX idx_reports_reporter ON reports(reporter_id);
CREATE TRIGGER trg_reports_updated_at BEFORE UPDATE ON reports FOR EACH ROW EXECUTE FUNCTION set_updated_at();

