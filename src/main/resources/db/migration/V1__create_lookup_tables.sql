-- ============================================================
-- V1 — Lookup / reference tables & Global Triggers
-- ============================================================

CREATE EXTENSION IF NOT EXISTS cube;
CREATE EXTENSION IF NOT EXISTS earthdistance;
CREATE EXTENSION IF NOT EXISTS btree_gist;
-- Global trigger function for updated_at
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

-- 1. Provinces
CREATE TABLE provinces (
    id         SERIAL       PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    code       VARCHAR(10)  NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_provinces_updated_at BEFORE UPDATE ON provinces FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 2. Districts
CREATE TABLE districts (
    id          SERIAL       PRIMARY KEY,
    province_id INT          NOT NULL REFERENCES provinces(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL,
    code        VARCHAR(10),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (province_id, slug)
);
CREATE INDEX idx_districts_province ON districts(province_id);
CREATE TRIGGER trg_districts_updated_at BEFORE UPDATE ON districts FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 3. Wards
CREATE TABLE wards (
    id          SERIAL       PRIMARY KEY,
    district_id INT          NOT NULL REFERENCES districts(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL,
    code        VARCHAR(10),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (district_id, slug)
);
CREATE INDEX idx_wards_district ON wards(district_id);
CREATE TRIGGER trg_wards_updated_at BEFORE UPDATE ON wards FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 4. Property types
CREATE TABLE property_types (
    id         SERIAL       PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    icon       VARCHAR(255),
    sort_order SMALLINT     NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_property_types_updated_at BEFORE UPDATE ON property_types FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 5. Amenities
CREATE TABLE amenities (
    id         SERIAL       PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    icon       VARCHAR(255),
    category   VARCHAR(50),
    sort_order SMALLINT     NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_amenities_updated_at BEFORE UPDATE ON amenities FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Seed data for lookups
INSERT INTO property_types (name, slug, icon, sort_order) VALUES
    ('Nhà trọ / Phòng trọ',    'nha-tro',        'home',           1),
    ('Căn hộ / Chung cư',      'can-ho',          'building',       2),
    ('Nhà nguyên căn',         'nha-nguyen-can',  'house',          3),
    ('Biệt thự / Villa',       'biet-thu',        'villa',          4),
    ('Văn phòng',              'van-phong',        'briefcase',      5),
    ('Mặt bằng / Kiot',        'mat-bang',         'store',          6),
    ('Đất nền',                'dat-nen',          'map',            7),
    ('Nhà mặt phố',            'nha-mat-pho',     'building-store', 8);

INSERT INTO amenities (name, slug, icon, category, sort_order) VALUES
    ('Camera an ninh',    'camera',      'camera',          'security',   1),
    ('Bảo vệ 24/7',       'bao-ve',      'shield-check',    'security',   2),
    ('Khóa cửa thông minh','khoa-thong-minh','lock',        'security',   3),
    ('Điều hòa',          'dieu-hoa',    'snowflake',       'furniture',  10),
    ('Máy giặt',          'may-giat',    'washing-machine', 'furniture',  11),
    ('Tủ lạnh',           'tu-lanh',     'fridge',          'furniture',  12),
    ('Giường ngủ',        'giuong',      'bed',             'furniture',  13),
    ('Tivi',              'tivi',        'device-tv',       'furniture',  14),
    ('Bàn làm việc',      'ban-lam-viec','desk',            'furniture',  15),
    ('Wifi miễn phí',     'wifi',        'wifi',            'utility',    20),
    ('Chỗ để xe máy',     'cho-xe-may',  'motorbike',       'utility',    21),
    ('Chỗ để ô tô',       'cho-o-to',    'car',             'utility',    22),
    ('Thang máy',         'thang-may',   'elevator',        'utility',    23),
    ('Bể bơi',            'be-boi',      'swimming-pool',   'utility',    24),
    ('Gym / Phòng tập',   'gym',         'barbell',         'utility',    25),
    ('Ban công',          'ban-cong',    'balcony',         'utility',    26),
    ('WC riêng',          'wc-rieng',    'toilet-paper',    'bathroom',   30),
    ('Bếp riêng',         'bep-rieng',   'chef-hat',        'kitchen',    31),
    ('Nước nóng',         'nuoc-nong',   'droplet',         'bathroom',   32);