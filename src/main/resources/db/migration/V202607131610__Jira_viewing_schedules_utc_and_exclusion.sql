-- JIRA: Prevent viewing schedule double-booking race conditions
-- - Store explicit UTC columns: scheduled_utc_time, scheduled_end_utc_time (TIMESTAMPTZ)
-- - Rename active pending status to PENDING_CONFIRMATION (migrate existing data)
-- - Add listing-level exclusion constraint on tstzrange(utc_start, utc_end) for active statuses

-- 1) Add explicit UTC columns (immutable, indexable)
ALTER TABLE viewing_schedules
    ADD COLUMN IF NOT EXISTS scheduled_utc_time TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS scheduled_end_utc_time TIMESTAMPTZ;

-- Backfill from existing UTC columns if present
UPDATE viewing_schedules
SET scheduled_utc_time = scheduled_start,
    scheduled_end_utc_time = scheduled_end
WHERE scheduled_utc_time IS NULL
   OR scheduled_end_utc_time IS NULL;

ALTER TABLE viewing_schedules
    ALTER COLUMN scheduled_utc_time SET NOT NULL,
    ALTER COLUMN scheduled_end_utc_time SET NOT NULL;

-- 2) Migrate status values: PENDING -> PENDING_CONFIRMATION
UPDATE viewing_schedules
SET status = 'PENDING_CONFIRMATION'
WHERE status = 'PENDING';

-- Drop the old implicit check constraint name if it exists (Postgres default naming)
ALTER TABLE viewing_schedules
    DROP CONSTRAINT IF EXISTS viewing_schedules_status_check;

-- Re-add explicit status check with JIRA-aligned pending name
ALTER TABLE viewing_schedules
    ADD CONSTRAINT chk_viewing_schedules_status
    CHECK (status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW'));

-- 3) Replace any prior listing-level exclusion constraint and recreate per JIRA
ALTER TABLE viewing_schedules
    DROP CONSTRAINT IF EXISTS no_listing_double_booking,
    DROP CONSTRAINT IF EXISTS no_client_double_booking,
    DROP CONSTRAINT IF EXISTS no_host_double_booking;

ALTER TABLE viewing_schedules
    ADD CONSTRAINT no_listing_double_booking
    EXCLUDE USING gist (
        listing_id WITH =,
        tstzrange(scheduled_utc_time, scheduled_end_utc_time) WITH &&
    )
    WHERE (status IN ('PENDING_CONFIRMATION', 'CONFIRMED') AND deleted_at IS NULL);

CREATE INDEX IF NOT EXISTS idx_schedules_utc_start ON viewing_schedules(scheduled_utc_time);

