-- ============================================================
-- V8 — Additional Indexes & Views
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_listings_address_trgm ON listings USING gin(address gin_trgm_ops) WHERE status = 'APPROVED';
CREATE INDEX idx_provinces_name_trgm ON provinces USING gin(name gin_trgm_ops);
CREATE INDEX idx_districts_name_trgm ON districts USING gin(name gin_trgm_ops);
CREATE INDEX idx_wards_name_trgm     ON wards     USING gin(name gin_trgm_ops);

CREATE INDEX idx_listings_pending   ON listings(created_at DESC) WHERE status = 'PENDING';
CREATE INDEX idx_listings_expiring  ON listings(expired_at ASC) WHERE status = 'APPROVED' AND expired_at IS NOT NULL;
CREATE INDEX idx_reports_pending    ON reports(created_at DESC)   WHERE status = 'PENDING';
CREATE INDEX idx_schedules_today    ON viewing_schedules(scheduled_start) WHERE status IN ('PENDING', 'CONFIRMED');

CREATE INDEX idx_listings_ward_type_status ON listings(ward_id, listing_type, status, normalized_price_vnd DESC);

CREATE INDEX idx_schedules_client_date ON viewing_schedules(client_id, scheduled_start DESC, status);

CREATE INDEX idx_conv_client_unread ON conversations(client_id, last_message_at DESC) WHERE client_unread_count > 0 AND client_deleted_at IS NULL;
CREATE INDEX idx_conv_host_unread  ON conversations(host_id, last_message_at DESC) WHERE host_unread_count  > 0 AND host_deleted_at  IS NULL;

CREATE OR REPLACE VIEW listing_stats AS
SELECT
    l.id, l.title, l.normalized_price_vnd AS price, 
    l.area, l.bedrooms, l.bathrooms, l.property_type_id, img.thumbnail_url,
    COALESCE(fs.fav_count, 0) AS favorite_count,
    COALESCE(rs.review_count, 0) AS review_count,
    rs.avg_rating,
    rs.avg_location_rating,
    rs.avg_accuracy_rating,
    rs.avg_host_rating,
    COALESCE(ss.completed_viewings, 0) AS completed_viewings
FROM listings l
LEFT JOIN (
    SELECT listing_id, thumbnail_url 
    FROM listing_images 
    WHERE is_primary = TRUE
) img ON img.listing_id = l.id
LEFT JOIN (SELECT listing_id, COUNT(id) AS fav_count FROM favorites GROUP BY listing_id) fs ON fs.listing_id = l.id
LEFT JOIN (
    SELECT 
        listing_id, 
        COUNT(id) AS review_count, 
        ROUND(AVG(rating), 1) AS avg_rating,
        ROUND(AVG(location_rating), 1) AS avg_location_rating,
        ROUND(AVG(accuracy_rating), 1) AS avg_accuracy_rating,
        ROUND(AVG(host_rating), 1) AS avg_host_rating
    FROM reviews 
    WHERE NOT is_hidden AND deleted_at IS NULL AND schedule_id IS NOT NULL 
    GROUP BY listing_id
) rs ON rs.listing_id = l.id
LEFT JOIN (SELECT listing_id, COUNT(id) AS completed_viewings FROM viewing_schedules WHERE status = 'COMPLETED' AND deleted_at IS NULL GROUP BY listing_id) ss ON ss.listing_id = l.id
WHERE l.status = 'APPROVED' AND l.deleted_at IS NULL;


CREATE MATERIALIZED VIEW admin_dashboard_stats AS
SELECT
    1 AS id,
    (SELECT COUNT(*) FROM users   WHERE status = 'ACTIVE' AND deleted_at IS NULL)           AS active_users,
    (SELECT COUNT(*) FROM listings WHERE status = 'APPROVED' AND deleted_at IS NULL)         AS active_listings,
    (SELECT COUNT(*) FROM listings WHERE status = 'PENDING' AND deleted_at IS NULL)          AS pending_listings,
    (SELECT COUNT(*) FROM viewing_schedules WHERE status = 'PENDING' AND deleted_at IS NULL) AS pending_schedules,
    (SELECT COUNT(*) FROM reports  WHERE status = 'PENDING')          AS pending_reports,
    (SELECT COUNT(*) FROM listings WHERE created_at >= now() - interval '7 days' AND deleted_at IS NULL) AS new_listings_7d,
    (SELECT COUNT(*) FROM users    WHERE created_at >= now() - interval '7 days' AND deleted_at IS NULL) AS new_users_7d,
    now() AS refreshed_at;

CREATE UNIQUE INDEX idx_admin_stats ON admin_dashboard_stats(id);

CREATE OR REPLACE FUNCTION refresh_admin_dashboard_stats()
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY admin_dashboard_stats;
END;
$$;

CREATE OR REPLACE FUNCTION cascade_user_soft_delete()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL THEN
        UPDATE listings SET status = 'SUSPENDED', suspend_reason = 'Owner account deactivated.' 
        WHERE owner_id = NEW.id AND status = 'APPROVED';
        
        UPDATE viewing_schedules SET status = 'CANCELLED', cancel_reason = 'User account deactivated.' 
        WHERE (client_id = NEW.id OR host_id = NEW.id) AND status IN ('PENDING', 'CONFIRMED');
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_cascade_user_soft_delete 
AFTER UPDATE OF deleted_at ON users 
FOR EACH ROW EXECUTE FUNCTION cascade_user_soft_delete();

