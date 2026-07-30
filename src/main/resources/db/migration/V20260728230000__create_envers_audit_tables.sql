CREATE SEQUENCE revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE revinfo (
    rev INTEGER PRIMARY KEY DEFAULT nextval('revinfo_seq'),
    revtstmp BIGINT
);

CREATE TABLE listings_aud (
    id UUID NOT NULL,
    rev INTEGER NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT NOT NULL,
    title VARCHAR(255),
    slug VARCHAR(255),
    description VARCHAR(2000),
    address VARCHAR(500),
    area NUMERIC(8,2),
    bedrooms INTEGER,
    bathrooms INTEGER,
    floors INTEGER,
    status VARCHAR(255),
    listing_type VARCHAR(255),
    owner_id UUID,
    agent_id UUID,
    property_type_id INTEGER,
    ward_code VARCHAR(255),
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
