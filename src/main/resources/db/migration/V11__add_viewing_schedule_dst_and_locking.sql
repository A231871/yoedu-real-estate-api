
ALTER TABLE viewing_schedules
    ADD COLUMN IF NOT EXISTS scheduled_local_time TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS timezone_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

UPDATE viewing_schedules
SET timezone_id = 'Asia/Ho_Chi_Minh',
    scheduled_local_time = scheduled_start AT TIME ZONE 'Asia/Ho_Chi_Minh'
WHERE timezone_id IS NULL;


ALTER TABLE viewing_schedules
    ALTER COLUMN scheduled_local_time SET NOT NULL,
ALTER COLUMN timezone_id SET NOT NULL,
ALTER COLUMN version SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_schedules_start_utc ON viewing_schedules(scheduled_start);