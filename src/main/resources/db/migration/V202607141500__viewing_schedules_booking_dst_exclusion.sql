-- Consolidated: viewing_schedules booking (DST local time, UTC columns, exclusion constraint)
-- Replaces V202607081450, V202607131405, V202607131610

-- 1) Drop V4 client/host exclusions (wrong granularity)
ALTER TABLE viewing_schedules
    DROP CONSTRAINT IF EXISTS no_client_double_booking,
    DROP CONSTRAINT IF EXISTS no_host_double_booking,
    DROP CONSTRAINT IF EXISTS no_listing_double_booking;

-- 2) Add all new columns in one ALTER
ALTER TABLE viewing_schedules
    ADD COLUMN IF NOT EXISTS scheduled_local_time TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS timezone_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS scheduled_utc_time TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS scheduled_end_utc_time TIMESTAMPTZ;

-- 3) Backfill existing rows
UPDATE viewing_schedules
SET timezone_id = COALESCE(timezone_id, 'Asia/Ho_Chi_Minh'),
    scheduled_local_time = COALESCE(
        scheduled_local_time,
        scheduled_start AT TIME ZONE 'Asia/Ho_Chi_Minh'
    ),
    scheduled_utc_time = COALESCE(scheduled_utc_time, scheduled_start),
    scheduled_end_utc_time = COALESCE(scheduled_end_utc_time, scheduled_end)
WHERE timezone_id IS NULL
   OR scheduled_local_time IS NULL
   OR scheduled_utc_time IS NULL
   OR scheduled_end_utc_time IS NULL;

-- 4) NOT NULL constraints
ALTER TABLE viewing_schedules
    ALTER COLUMN scheduled_local_time SET NOT NULL,
    ALTER COLUMN timezone_id SET NOT NULL,
    ALTER COLUMN scheduled_utc_time SET NOT NULL,
    ALTER COLUMN scheduled_end_utc_time SET NOT NULL,
    ALTER COLUMN version SET NOT NULL;

-- 5) Status: PENDING -> PENDING_CONFIRMATION
UPDATE viewing_schedules
SET status = 'PENDING_CONFIRMATION'
WHERE status = 'PENDING';

ALTER TABLE viewing_schedules
    DROP CONSTRAINT IF EXISTS viewing_schedules_status_check;

ALTER TABLE viewing_schedules
    ADD CONSTRAINT chk_viewing_schedules_status
    CHECK (status IN (
        'PENDING_CONFIRMATION',
        'CONFIRMED',
        'CANCELLED',
        'COMPLETED',
        'NO_SHOW'
    ));

-- 6) Soft-reference pattern (modulith)
ALTER TABLE viewing_schedules
    DROP CONSTRAINT IF EXISTS viewing_schedules_listing_id_fkey;

-- 7) Final listing-level exclusion (immutable UTC range, partial index)
ALTER TABLE viewing_schedules
    ADD CONSTRAINT no_listing_double_booking
    EXCLUDE USING gist (
        listing_id WITH =,
        tstzrange(scheduled_utc_time, scheduled_end_utc_time) WITH &&
    )
    WHERE (
        status IN ('PENDING_CONFIRMATION', 'CONFIRMED')
        AND deleted_at IS NULL
    );

-- 8) Index for cron/query performance (avoid full table scans)
CREATE INDEX IF NOT EXISTS idx_schedules_utc_start
    ON viewing_schedules(scheduled_utc_time);

-- 9) Update check_listing_bookable() for PENDING_CONFIRMATION + UTC columns
CREATE OR REPLACE FUNCTION check_listing_bookable()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF (TG_OP = 'INSERT' OR
        NEW.scheduled_utc_time IS DISTINCT FROM OLD.scheduled_utc_time OR
        NEW.scheduled_end_utc_time IS DISTINCT FROM OLD.scheduled_end_utc_time OR
        (TG_OP = 'UPDATE'
            AND NEW.status IN ('PENDING_CONFIRMATION', 'CONFIRMED')
            AND OLD.status NOT IN ('PENDING_CONFIRMATION', 'CONFIRMED'))) THEN

        IF NOT EXISTS (
            SELECT 1 FROM listings
            WHERE id = NEW.listing_id
              AND status = 'APPROVED'
              AND deleted_at IS NULL
        ) THEN
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

    IF NEW.scheduled_utc_time < now()
        AND (TG_OP = 'INSERT'
            OR NEW.scheduled_utc_time IS DISTINCT FROM OLD.scheduled_utc_time) THEN
        RAISE EXCEPTION 'Cannot schedule a viewing in the past';
    END IF;

    IF NEW.status = 'COMPLETED' AND NEW.scheduled_utc_time > now() THEN
        RAISE EXCEPTION 'Cannot complete a viewing schedule in the future';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_check_listing_bookable ON viewing_schedules;

CREATE TRIGGER trg_check_listing_bookable
    BEFORE INSERT OR UPDATE OF
        scheduled_utc_time,
        scheduled_end_utc_time,
        scheduled_start,
        scheduled_end,
        status
    ON viewing_schedules
    FOR EACH ROW
    EXECUTE FUNCTION check_listing_bookable();
