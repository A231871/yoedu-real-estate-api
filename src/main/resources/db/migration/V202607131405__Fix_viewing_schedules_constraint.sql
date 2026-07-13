-- Drop the incorrect host/client level exclusion constraints
ALTER TABLE viewing_schedules
    DROP CONSTRAINT IF EXISTS no_client_double_booking,
    DROP CONSTRAINT IF EXISTS no_host_double_booking;

-- Add the correct listing level exclusion constraint as per the architecture plan
-- This allows Real Estate Agencies to show multiple different properties concurrently via different agents
-- but prevents double-booking the same listing at the same time.
ALTER TABLE viewing_schedules
    ADD CONSTRAINT no_listing_double_booking EXCLUDE USING gist (
        listing_id WITH =,
        tstzrange(scheduled_start, scheduled_end) WITH &&
    ) WHERE (status IN ('PENDING', 'CONFIRMED'));
