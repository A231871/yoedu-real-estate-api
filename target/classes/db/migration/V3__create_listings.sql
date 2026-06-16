-- ============================================================
-- V3 — Listings
-- ============================================================

CREATE TABLE listings (
    id               UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id         UUID              NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    agent_id         UUID              REFERENCES users(id) ON DELETE SET NULL,

    property_type_id INT               NOT NULL REFERENCES property_types(id),
    listing_type     VARCHAR(50)       NOT NULL CHECK (listing_type IN ('FOR_RENT', 'FOR_SALE')),

    ward_id          INT               NOT NULL REFERENCES wards(id),
    address          VARCHAR(500)      NOT NULL,
    latitude         NUMERIC(10, 7),
    longitude        NUMERIC(10, 7),

    title            VARCHAR(300)      NOT NULL,
    description      TEXT              NOT NULL,
    slug             VARCHAR(350)      NOT NULL UNIQUE,

    price            NUMERIC(18, 0)    NOT NULL CHECK (price >= 0),
    price_unit       VARCHAR(50)       NOT NULL DEFAULT 'VND_MONTH' CHECK (price_unit IN ('VND_MONTH', 'VND_TOTAL', 'USD_MONTH', 'USD_TOTAL')),
    area             NUMERIC(8, 2)     CHECK (area > 0),
    bedrooms         SMALLINT          CHECK (bedrooms >= 0),
    bathrooms        SMALLINT          CHECK (bathrooms >= 0),
    floors           SMALLINT          CHECK (floors >= 0),
    direction        VARCHAR(50)       CHECK (direction IN ('EAST', 'WEST', 'SOUTH', 'NORTH', 'NORTHEAST', 'NORTHWEST', 'SOUTHEAST', 'SOUTHWEST')),
    year_built       SMALLINT          CHECK (year_built >= 1900 AND year_built <= EXTRACT(YEAR FROM now()) + 2),
    interior_status  VARCHAR(50),

    status           VARCHAR(50)       NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'EXPIRED', 'CLOSED')),
    reject_reason    TEXT,
    approved_by      UUID              REFERENCES users(id) ON DELETE SET NULL,
    approved_at      TIMESTAMPTZ,
    expired_at       TIMESTAMPTZ,

    meta_title       VARCHAR(300),
    meta_description VARCHAR(500),

    view_count       INT               NOT NULL DEFAULT 0 CHECK (view_count >= 0),
    favorite_count   INT               NOT NULL DEFAULT 0 CHECK (favorite_count >= 0),

    created_at       TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ       NOT NULL DEFAULT now(),
    search_vector    TSVECTOR
);

CREATE INDEX idx_listings_search ON listings (ward_id, listing_type, status, price, area, bedrooms) WHERE status = 'APPROVED';
CREATE INDEX idx_listings_owner        ON listings(owner_id);
CREATE INDEX idx_listings_agent        ON listings(agent_id);
CREATE INDEX idx_listings_type         ON listings(property_type_id);
CREATE INDEX idx_listings_status       ON listings(status);
CREATE INDEX idx_listings_created      ON listings(created_at DESC);
CREATE INDEX idx_listings_price        ON listings(price) WHERE status = 'APPROVED';
CREATE INDEX idx_listings_search_vec   ON listings USING gin(search_vector);
CREATE INDEX idx_listings_location     ON listings USING gist(ll_to_earth(latitude::float8, longitude::float8)) WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

CREATE OR REPLACE FUNCTION update_listing_search_vector()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('simple', coalesce(NEW.title, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(NEW.description, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(NEW.address, '')), 'C');
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_listings_search_vector BEFORE INSERT OR UPDATE OF title, description, address ON listings FOR EACH ROW EXECUTE FUNCTION update_listing_search_vector();
CREATE TRIGGER trg_listings_updated_at BEFORE UPDATE ON listings FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE listing_amenities (
    listing_id  UUID  NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    amenity_id  INT   NOT NULL REFERENCES amenities(id) ON DELETE CASCADE,
    PRIMARY KEY (listing_id, amenity_id)
);
CREATE INDEX idx_listing_amenities_amenity ON listing_amenities(amenity_id);

CREATE TABLE listing_images (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id    UUID         NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    url           VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    caption       VARCHAR(200),
    sort_order    SMALLINT     NOT NULL DEFAULT 0,
    is_primary    BOOLEAN      NOT NULL DEFAULT FALSE,
    width         INT,
    height        INT,
    file_size     INT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_listing_images_listing ON listing_images(listing_id, sort_order);
CREATE UNIQUE INDEX idx_listing_images_primary ON listing_images(listing_id) WHERE is_primary = TRUE;
CREATE TRIGGER trg_listing_images_updated_at BEFORE UPDATE ON listing_images FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE listing_views (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id  UUID         NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    user_id     UUID         REFERENCES users(id) ON DELETE SET NULL,
    ip_address  INET,
    user_agent  VARCHAR(500),
    viewed_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_listing_views_listing  ON listing_views(listing_id, viewed_at DESC);
CREATE INDEX idx_listing_views_user     ON listing_views(user_id) WHERE user_id IS NOT NULL;
CREATE UNIQUE INDEX idx_listing_views_dedup_user ON listing_views(listing_id, user_id, DATE(viewed_at)) WHERE user_id IS NOT NULL;
CREATE UNIQUE INDEX idx_listing_views_dedup_ip ON listing_views(listing_id, ip_address, DATE(viewed_at)) WHERE user_id IS NULL;

CREATE OR REPLACE FUNCTION increment_listing_view_count()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    UPDATE listings SET view_count = view_count + 1 WHERE id = NEW.listing_id;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_listing_view_count AFTER INSERT ON listing_views FOR EACH ROW EXECUTE FUNCTION increment_listing_view_count();