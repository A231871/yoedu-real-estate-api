-- Post-viewing state machine: REQUIRES_FEEDBACK + confirmation_prompted_at for 48h auto-complete cron
-- ShedLock table for distributed cron locking

ALTER TABLE viewing_schedules
    ADD COLUMN IF NOT EXISTS confirmation_prompted_at TIMESTAMPTZ;

ALTER TABLE viewing_schedules
    DROP CONSTRAINT IF EXISTS chk_viewing_schedules_status;

ALTER TABLE viewing_schedules
    ADD CONSTRAINT chk_viewing_schedules_status
    CHECK (status IN (
        'PENDING_CONFIRMATION',
        'CONFIRMED',
        'REQUIRES_FEEDBACK',
        'CANCELLED',
        'COMPLETED',
        'NO_SHOW'
    ));

-- Cron 1: find past CONFIRMED schedules by indexed UTC end time
CREATE INDEX IF NOT EXISTS idx_schedules_end_utc_cron
    ON viewing_schedules(scheduled_end_utc_time)
    WHERE status = 'CONFIRMED' AND deleted_at IS NULL;

-- Cron 2: find stale REQUIRES_FEEDBACK by confirmation_prompted_at (48h window)
CREATE INDEX IF NOT EXISTS idx_schedules_feedback_cron
    ON viewing_schedules(confirmation_prompted_at)
    WHERE status = 'REQUIRES_FEEDBACK' AND deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMPTZ  NOT NULL,
    locked_at  TIMESTAMPTZ  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
