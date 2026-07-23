-- ============================================================
-- V202507231500 — Reviews: modulith soft-reference refactor
-- ============================================================

-- Drop the hard FK from reviews.listing_id → listings(id)
ALTER TABLE reviews DROP CONSTRAINT IF EXISTS reviews_listing_id_fkey;

-- Ensure a plain btree index exists for listing_id lookups (performance)
CREATE INDEX IF NOT EXISTS idx_reviews_listing_id ON reviews(listing_id);
