CREATE TABLE store_settings (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL UNIQUE,
    street varchar(180),
    street_number varchar(30),
    address_complement varchar(120),
    neighborhood varchar(120),
    city varchar(120),
    state_code varchar(2),
    postal_code varchar(9),
    latitude numeric(9,6),
    longitude numeric(9,6),
    accepts_pix boolean NOT NULL DEFAULT true,
    accepts_card boolean NOT NULL DEFAULT false,
    accepts_cash_on_delivery boolean NOT NULL DEFAULT false,
    delivery_fee numeric(10,2) NOT NULL DEFAULT 0 CHECK (delivery_fee >= 0),
    preparation_minutes integer NOT NULL DEFAULT 30 CHECK (preparation_minutes BETWEEN 0 AND 1440),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_store_settings_store_tenant
        FOREIGN KEY (organization_id, store_id) REFERENCES stores (organization_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_store_settings_location_pair
        CHECK (((latitude IS NULL) = (longitude IS NULL)) AND (latitude IS NULL OR (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180))),
    CONSTRAINT ck_store_settings_state_code
        CHECK (state_code IS NULL OR state_code ~ '^[A-Z]{2}$')
);
CREATE INDEX ix_store_settings_tenant ON store_settings (organization_id, store_id);

CREATE TABLE delivery_zones (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL UNIQUE,
    center_latitude numeric(9,6) NOT NULL CHECK (center_latitude BETWEEN -90 AND 90),
    center_longitude numeric(9,6) NOT NULL CHECK (center_longitude BETWEEN -180 AND 180),
    radius_meters integer NOT NULL CHECK (radius_meters BETWEEN 100 AND 100000),
    active boolean NOT NULL DEFAULT false,
    policy_version integer NOT NULL DEFAULT 1 CHECK (policy_version > 0),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_delivery_zones_store_tenant
        FOREIGN KEY (organization_id, store_id) REFERENCES stores (organization_id, id) ON DELETE RESTRICT
);
CREATE INDEX ix_delivery_zones_tenant_active ON delivery_zones (organization_id, store_id, active);

CREATE TABLE catalog_categories (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    name varchar(100) NOT NULL,
    display_order integer NOT NULL DEFAULT 0,
    active boolean NOT NULL DEFAULT true,
    archived_at timestamptz,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_categories_tenant_id UNIQUE (organization_id, store_id, id),
    CONSTRAINT fk_categories_store_tenant
        FOREIGN KEY (organization_id, store_id) REFERENCES stores (organization_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_categories_display_order CHECK (display_order >= 0)
);
CREATE INDEX ix_categories_tenant_active_order ON catalog_categories (organization_id, store_id, active, display_order);

CREATE TABLE products (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    category_id uuid,
    name varchar(140) NOT NULL,
    description varchar(2000),
    price numeric(12,2) NOT NULL CHECK (price >= 0),
    active boolean NOT NULL DEFAULT true,
    archived_at timestamptz,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_products_store_tenant
        FOREIGN KEY (organization_id, store_id) REFERENCES stores (organization_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_products_category_tenant
        FOREIGN KEY (organization_id, store_id, category_id)
        REFERENCES catalog_categories (organization_id, store_id, id) ON DELETE RESTRICT
);
CREATE INDEX ix_products_tenant_active ON products (organization_id, store_id, active, name);
CREATE INDEX ix_products_tenant_category ON products (organization_id, store_id, category_id);


