-- ============================================================
-- V3 — Listings
-- ============================================================

CREATE TABLE listings (
    id               UUID              PRIMARY KEY DEFAULT uuidv7(),
    owner_id         UUID              NOT NULL REFERENCES owner_profiles(user_id) ON DELETE CASCADE,
    agent_id         UUID              REFERENCES agent_profiles(user_id) ON DELETE SET NULL,

    property_type_id INT               NOT NULL REFERENCES property_types(id),
    listing_type     VARCHAR(50)       NOT NULL CHECK (listing_type IN ('FOR_RENT', 'FOR_SALE')),
    package_id       INT               REFERENCES listing_packages(id) ON DELETE SET NULL,
    priority_level   SMALLINT          NOT NULL DEFAULT 0,


    ward_id          INT               NOT NULL REFERENCES wards(id),
    address          VARCHAR(500)      NOT NULL,
    latitude         NUMERIC(10, 7),
    longitude        NUMERIC(10, 7),

    title            VARCHAR(300)      NOT NULL,
    description      TEXT              NOT NULL,
    slug             VARCHAR(350)      NOT NULL,

    price            NUMERIC(18, 0)    NOT NULL CHECK (price >= 0),
    price_unit       VARCHAR(50)       NOT NULL DEFAULT 'VND_MONTH' CHECK (price_unit IN ('VND_MONTH', 'VND_TOTAL', 'USD_MONTH', 'USD_TOTAL', 'VND_PER_M2')),
    CONSTRAINT chk_price_area_match CHECK (price_unit != 'VND_PER_M2' OR area IS NOT NULL),
    normalized_price_vnd NUMERIC(18, 0)    NOT NULL,

    deposit_amount   NUMERIC(18, 0)    CHECK (deposit_amount >= 0),
    deposit_unit     VARCHAR(50)       NOT NULL DEFAULT 'VND_TOTAL' CHECK (deposit_unit IN ('VND_MONTH', 'VND_TOTAL', 'USD_MONTH', 'USD_TOTAL')),
    available_from   DATE,

    area             NUMERIC(8, 2)     CHECK (area > 0),
    bedrooms         SMALLINT          CHECK (bedrooms >= 0),
    bathrooms        SMALLINT          CHECK (bathrooms >= 0),
    floors           SMALLINT          CHECK (floors >= 0),
    direction        VARCHAR(50)       CHECK (direction IN ('EAST', 'WEST', 'SOUTH', 'NORTH', 'NORTHEAST', 'NORTHWEST', 'SOUTHEAST', 'SOUTHWEST')),
    year_built       SMALLINT          CHECK (year_built >= 1900 AND year_built <= 2100),
    interior_status  VARCHAR(50),
    legal_status     VARCHAR(100),
    frontage_width   NUMERIC(8, 2),
    road_width       NUMERIC(8, 2),

    status           VARCHAR(50)       NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'EXPIRED', 'CLOSED', 'SUSPENDED')),
    reject_reason    TEXT,
    approved_by      UUID              REFERENCES users(id) ON DELETE SET NULL,
    approved_at      TIMESTAMPTZ,
    expired_at       TIMESTAMPTZ,
    suspended_by     UUID              REFERENCES users(id) ON DELETE SET NULL,
    suspend_reason   TEXT,

    CONSTRAINT chk_status_audit CHECK (
        (status = 'APPROVED' AND approved_by IS NOT NULL AND approved_at IS NOT NULL) OR 
        (status = 'REJECTED' AND reject_reason IS NOT NULL) OR 
        (status = 'SUSPENDED' AND suspended_by IS NOT NULL AND suspend_reason IS NOT NULL) OR 
        status IN ('DRAFT', 'PENDING', 'CLOSED', 'EXPIRED')
    ),

    meta_title       VARCHAR(300),
    meta_description VARCHAR(500),

    deleted_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ       NOT NULL DEFAULT now(),
    search_vector    TSVECTOR,
    CONSTRAINT uq_listing_id_type UNIQUE (id, listing_type),
    CONSTRAINT chk_valid_coordinates CHECK (
        (latitude IS NULL AND longitude IS NULL) OR 
        (latitude BETWEEN -90.0000000 AND 90.0000000 AND longitude BETWEEN -180.0000000 AND 180.0000000)
    )
);

CREATE INDEX idx_listings_search ON listings USING btree (ward_id, bedrooms, normalized_price_vnd, area) WHERE status = 'APPROVED' AND deleted_at IS NULL;

CREATE INDEX idx_listings_slug ON listings(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_listings_owner        ON listings(owner_id);
CREATE INDEX idx_listings_agent        ON listings(agent_id);
CREATE INDEX idx_listings_type         ON listings(property_type_id);
CREATE INDEX idx_listings_status       ON listings(status);
CREATE INDEX idx_listings_created      ON listings(created_at DESC, id DESC);
CREATE INDEX idx_listings_price        ON listings(normalized_price_vnd) WHERE status = 'APPROVED' AND deleted_at IS NULL;
CREATE INDEX idx_listings_search_vec   ON listings USING gin(search_vector);
CREATE INDEX idx_listings_location     ON listings USING gist(ll_to_earth(latitude::float8, longitude::float8)) WHERE latitude IS NOT NULL AND longitude IS NOT NULL;
CREATE OR REPLACE FUNCTION update_listing_search_vector()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    prov_name VARCHAR;
    dist_name VARCHAR;
    ward_name VARCHAR;
BEGIN
    SELECT w.name, d.name, p.name INTO ward_name, dist_name, prov_name
    FROM wards w
    JOIN districts d ON w.district_id = d.id
    JOIN provinces p ON d.province_id = p.id
    WHERE w.id = NEW.ward_id;

    NEW.search_vector :=
        setweight(to_tsvector('simple', unaccent(coalesce(NEW.title, ''))), 'A') ||
        setweight(to_tsvector('simple', unaccent(coalesce(NEW.description, ''))), 'B') ||
        setweight(to_tsvector('simple', unaccent(coalesce(NEW.address, '') || ' ' || coalesce(ward_name, '') || ' ' || coalesce(dist_name, '') || ' ' || coalesce(prov_name, ''))), 'C');
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_listings_search_vector BEFORE INSERT OR UPDATE OF title, description, address, ward_id ON listings FOR EACH ROW EXECUTE FUNCTION update_listing_search_vector();

CREATE OR REPLACE FUNCTION reset_listing_approval()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status = 'APPROVED' AND (
        NEW.title IS DISTINCT FROM OLD.title OR 
        NEW.description IS DISTINCT FROM OLD.description OR 
        NEW.price IS DISTINCT FROM OLD.price OR
        NEW.area IS DISTINCT FROM OLD.area
    ) THEN
        NEW.status := 'PENDING';
        NEW.approved_by := NULL;
        NEW.approved_at := NULL;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_reset_listing_approval 
BEFORE UPDATE OF title, description, price, area 
ON listings 
FOR EACH ROW EXECUTE FUNCTION reset_listing_approval();

CREATE TRIGGER trg_listings_updated_at BEFORE UPDATE ON listings FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE listing_amenities (
    id          UUID  PRIMARY KEY DEFAULT uuidv7(),
    listing_id  UUID  NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    amenity_id  INT   NOT NULL REFERENCES amenities(id) ON DELETE RESTRICT,
    UNIQUE (listing_id, amenity_id)
);
CREATE INDEX idx_listing_amenities_amenity ON listing_amenities(amenity_id);
CREATE TABLE listing_images (
    id            UUID         PRIMARY KEY DEFAULT uuidv7(),
    listing_id    UUID         NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    media_type    VARCHAR(50)  NOT NULL DEFAULT 'IMAGE' CHECK (media_type IN ('IMAGE', 'VIDEO', '3D_TOUR')),
    url           VARCHAR(500) NOT NULL,
    video_url     VARCHAR(500),
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
    id          UUID         PRIMARY KEY DEFAULT uuidv7(),
    listing_id  UUID         NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    user_id     UUID         REFERENCES users(id) ON DELETE SET NULL,
    ip_address  INET,
    user_agent  VARCHAR(500),
    viewed_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_listing_views_daily ON listing_views (listing_id, ip_address, ((viewed_at AT TIME ZONE 'UTC')::DATE));
CREATE INDEX idx_listing_views_listing  ON listing_views(listing_id, viewed_at DESC);
CREATE INDEX idx_listing_views_user     ON listing_views(user_id) WHERE user_id IS NOT NULL;

CREATE TABLE saved_searches (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    filters JSONB NOT NULL,
    notify_email BOOLEAN NOT NULL DEFAULT FALSE,
    notify_push BOOLEAN NOT NULL DEFAULT FALSE,
    last_notified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_saved_searches_user ON saved_searches(user_id);
CREATE TRIGGER trg_saved_searches_updated_at BEFORE UPDATE ON saved_searches FOR EACH ROW EXECUTE FUNCTION set_updated_at();
