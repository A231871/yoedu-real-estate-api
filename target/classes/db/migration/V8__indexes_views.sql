-- ============================================================
-- V8 — Additional Indexes & Views
-- ============================================================

CREATE EXTENSION IF NOT EXISTS cube;
CREATE EXTENSION IF NOT EXISTS earthdistance;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_listings_address_trgm ON listings USING gin(address gin_trgm_ops) WHERE status = 'APPROVED';
CREATE INDEX idx_provinces_name_trgm ON provinces USING gin(name gin_trgm_ops);
CREATE INDEX idx_districts_name_trgm ON districts USING gin(name gin_trgm_ops);
CREATE INDEX idx_wards_name_trgm     ON wards     USING gin(name gin_trgm_ops);

CREATE INDEX idx_listings_pending   ON listings(created_at DESC) WHERE status = 'PENDING';
CREATE INDEX idx_listings_expiring  ON listings(expired_at ASC) WHERE status = 'APPROVED' AND expired_at IS NOT NULL;
CREATE INDEX idx_reports_pending    ON reports(created_at DESC)   WHERE status = 'PENDING';
CREATE INDEX idx_schedules_today    ON viewing_schedules(scheduled_date, scheduled_time) WHERE status IN ('PENDING', 'CONFIRMED');

CREATE INDEX idx_listings_ward_type_status ON listings(ward_id, listing_type, status, price DESC);
CREATE INDEX idx_favorites_user_created ON favorites(user_id, created_at DESC);
CREATE INDEX idx_schedules_renter_date ON viewing_schedules(renter_id, scheduled_date DESC, status);

CREATE INDEX idx_conv_renter_unread ON conversations(renter_id, last_message_at DESC) WHERE renter_unread_count > 0 AND renter_deleted_at IS NULL;
CREATE INDEX idx_conv_host_unread  ON conversations(host_id, last_message_at DESC) WHERE host_unread_count  > 0 AND host_deleted_at  IS NULL;

CREATE VIEW listing_stats AS
SELECT
    l.id, l.title, l.price, l.view_count, l.favorite_count,
    COUNT(r.id)               AS review_count,
    ROUND(AVG(r.rating), 1)   AS avg_rating,
    COUNT(vs.id) FILTER (WHERE vs.status = 'COMPLETED') AS completed_viewings
FROM listings l
LEFT JOIN reviews r ON r.listing_id = l.id AND r.is_hidden = FALSE
LEFT JOIN viewing_schedules vs ON vs.listing_id = l.id
WHERE l.status = 'APPROVED'
GROUP BY l.id;

CREATE VIEW user_inbox AS
SELECT
    c.id                AS conversation_id,
    c.listing_id,
    l.title             AS listing_title,
    li.url              AS listing_thumbnail,
    c.renter_id,
    c.host_id,
    c.last_message_at,
    c.last_message_preview,
    'RENTER'            AS role,
    c.renter_id         AS user_id,
    c.renter_unread_count AS unread_count
FROM conversations c
JOIN listings l ON l.id = c.listing_id
LEFT JOIN listing_images li ON li.listing_id = l.id AND li.is_primary = TRUE
WHERE c.renter_deleted_at IS NULL
UNION ALL
SELECT
    c.id,
    c.listing_id,
    l.title,
    li.url,
    c.renter_id,
    c.host_id,
    c.last_message_at,
    c.last_message_preview,
    'HOST'              AS role,
    c.host_id           AS user_id,
    c.host_unread_count AS unread_count
FROM conversations c
JOIN listings l ON l.id = c.listing_id
LEFT JOIN listing_images li ON li.listing_id = l.id AND li.is_primary = TRUE
WHERE c.host_deleted_at IS NULL;

CREATE MATERIALIZED VIEW admin_dashboard_stats AS
SELECT
    (SELECT COUNT(*) FROM users   WHERE status = 'ACTIVE')           AS active_users,
    (SELECT COUNT(*) FROM listings WHERE status = 'APPROVED')         AS active_listings,
    (SELECT COUNT(*) FROM listings WHERE status = 'PENDING')          AS pending_listings,
    (SELECT COUNT(*) FROM viewing_schedules WHERE status = 'PENDING') AS pending_schedules,
    (SELECT COUNT(*) FROM reports  WHERE status = 'PENDING')          AS pending_reports,
    (SELECT COUNT(*) FROM listings WHERE created_at >= now() - interval '7 days') AS new_listings_7d,
    (SELECT COUNT(*) FROM users    WHERE created_at >= now() - interval '7 days') AS new_users_7d,
    now() AS refreshed_at;

CREATE UNIQUE INDEX idx_admin_stats ON admin_dashboard_stats(refreshed_at);