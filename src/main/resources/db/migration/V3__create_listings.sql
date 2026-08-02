-- ============================================================
-- V3 — Listings
-- ============================================================

CREATE TABLE listings (
    id                UUID PRIMARY KEY DEFAULT uuidv7(),

    owner_id          UUID NOT NULL
        REFERENCES users(id) ON DELETE CASCADE,

    agent_id          UUID
        REFERENCES users(id) ON DELETE SET NULL,

    property_type_id  INT NOT NULL
        REFERENCES property_types(id),

    address           VARCHAR(500) NOT NULL,

    ward_code         VARCHAR(20) NOT NULL
        REFERENCES wards(code),

    listing_type      VARCHAR(50) NOT NULL
        CHECK (listing_type IN ('FOR_RENT', 'FOR_SALE')),

    title             VARCHAR(255) NOT NULL,

    slug              VARCHAR(255) NOT NULL,

    description       VARCHAR(2000) NOT NULL,

    area              NUMERIC(8,2) NOT NULL
        CHECK (area > 0),

    bedrooms          INT
        CHECK (bedrooms >= 0),

    bathrooms         INT
        CHECK (bathrooms >= 0),

    floors            INT
        CHECK (floors >= 0),

    amount_vnd        NUMERIC(20, 2) NOT NULL
        CHECK (amount_vnd >= 0),

    status            VARCHAR(50) NOT NULL DEFAULT 'PENDING'
        CHECK (
            status IN (
                'DRAFT',
                'PENDING',
                'APPROVED',
                'REJECTED',
                'EXPIRED',
                'CLOSED',
                'SUSPENDED'
            )
        ),

    deleted_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- Listings Indexes
-- ============================================================

CREATE INDEX idx_listings_search
ON listings (ward_code, bedrooms, area)
WHERE status = 'APPROVED'
  AND deleted_at IS NULL;

CREATE INDEX idx_listings_owner
ON listings(owner_id);

CREATE INDEX idx_listings_agent
ON listings(agent_id);

CREATE INDEX idx_listings_type
ON listings(property_type_id);

CREATE INDEX idx_listings_status
ON listings(status);

CREATE INDEX idx_listings_created
ON listings(created_at DESC, id DESC);

CREATE TRIGGER trg_listings_updated_at
BEFORE UPDATE ON listings
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- Listing Amenities
-- ============================================================

DROP TABLE IF EXISTS listing_amenities CASCADE;

CREATE TABLE listing_amenities (
    listing_id UUID NOT NULL REFERENCES listings(id)  ON DELETE CASCADE,
    amenity_id INT  NOT NULL REFERENCES amenities(id) ON DELETE CASCADE,
    PRIMARY KEY (listing_id, amenity_id)
);

CREATE INDEX idx_listing_amenities_amenity ON listing_amenities(amenity_id);

-- ============================================================
-- Listing Media
-- ============================================================

CREATE TABLE listing_media (
    id            UUID PRIMARY KEY DEFAULT uuidv7(),

    listing_id    UUID NOT NULL
        REFERENCES listings(id) ON DELETE CASCADE,

    media_type    VARCHAR(50) NOT NULL DEFAULT 'IMAGE'
        CHECK (media_type IN ('IMAGE', 'VIDEO')),

    url           VARCHAR(500) NOT NULL,

    caption       VARCHAR(200),

    sort_order    INTEGER NOT NULL DEFAULT 0,

    is_primary    BOOLEAN NOT NULL DEFAULT FALSE,

    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_listing_media_listing
ON listing_media(listing_id, sort_order);

CREATE UNIQUE INDEX idx_listing_media_primary
ON listing_media(listing_id)
WHERE is_primary = TRUE;

CREATE TRIGGER trg_listing_media_updated_at
BEFORE UPDATE ON listing_media
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- ============================================================
-- Listing Views
-- ============================================================

CREATE TABLE listing_views (
    id          UUID PRIMARY KEY DEFAULT uuidv7(),

    listing_id  UUID NOT NULL
        REFERENCES listings(id) ON DELETE CASCADE,

    user_id     UUID
        REFERENCES users(id) ON DELETE SET NULL,

    ip_address  INET,

    user_agent  VARCHAR(500),

    viewed_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_listing_views_daily
ON listing_views (
    listing_id,
    ip_address,
    ((viewed_at AT TIME ZONE 'UTC')::DATE)
);

CREATE INDEX idx_listing_views_listing
ON listing_views(listing_id, viewed_at DESC);

CREATE INDEX idx_listing_views_user
ON listing_views(user_id)
WHERE user_id IS NOT NULL;

-- ============================================================
-- Saved Searches
-- ============================================================

CREATE TABLE saved_searches (
    id                  UUID PRIMARY KEY DEFAULT uuidv7(),

    user_id             UUID NOT NULL
        REFERENCES users(id) ON DELETE CASCADE,

    name                VARCHAR(200) NOT NULL,

    filters             JSONB NOT NULL,

    notify_email        BOOLEAN NOT NULL DEFAULT FALSE,

    notify_push         BOOLEAN NOT NULL DEFAULT FALSE,

    last_notified_at    TIMESTAMPTZ,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_saved_searches_user
ON saved_searches(user_id);

CREATE TRIGGER trg_saved_searches_updated_at
BEFORE UPDATE ON saved_searches
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
