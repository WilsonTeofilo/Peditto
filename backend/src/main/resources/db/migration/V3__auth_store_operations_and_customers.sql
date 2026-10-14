-- Security and store operations foundation. Financial and authentication secrets are never stored in plaintext.
CREATE TABLE user_sessions (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    family_id uuid NOT NULL, refresh_token_hash char(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL, last_used_at timestamptz,
    revoked_at timestamptz, revoke_reason varchar(40), replaced_by_session_id uuid REFERENCES user_sessions(id) ON DELETE RESTRICT,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_user_sessions_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_user_sessions_revoked CHECK ((revoked_at IS NULL) = (revoke_reason IS NULL))
);
CREATE INDEX ix_user_sessions_user_live ON user_sessions(user_id, expires_at) WHERE revoked_at IS NULL;
CREATE INDEX ix_user_sessions_family ON user_sessions(family_id);

CREATE TABLE password_reset_tokens (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    token_hash char(64) NOT NULL UNIQUE, expires_at timestamptz NOT NULL,
    consumed_at timestamptz, invalidated_at timestamptz,
    CONSTRAINT ck_password_reset_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_password_reset_terminal CHECK (NOT (consumed_at IS NOT NULL AND invalidated_at IS NOT NULL))
);
CREATE INDEX ix_password_reset_user_live ON password_reset_tokens(user_id, expires_at) WHERE consumed_at IS NULL AND invalidated_at IS NULL;

CREATE TABLE outbox_messages (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    aggregate_type varchar(80) NOT NULL, aggregate_id uuid NOT NULL,
    event_type varchar(100) NOT NULL, destination varchar(254) NOT NULL,
    payload_ciphertext bytea, encryption_key_version varchar(80) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('PENDING','PROCESSING','SENT','DEAD')),
    attempts smallint NOT NULL DEFAULT 0 CHECK (attempts BETWEEN 0 AND 100),
    available_at timestamptz NOT NULL, locked_until timestamptz,
    sent_at timestamptz, last_error_code varchar(80), deduplication_key varchar(160) NOT NULL UNIQUE,
    CONSTRAINT ck_outbox_sent CHECK ((status <> 'SENT') OR sent_at IS NOT NULL),
    CONSTRAINT ck_outbox_payload CHECK ((status = 'SENT') OR payload_ciphertext IS NOT NULL)
);
CREATE INDEX ix_outbox_pending ON outbox_messages(available_at, created_at) WHERE status IN ('PENDING','PROCESSING');

CREATE TABLE api_rate_limit_buckets (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    bucket_key char(64) NOT NULL UNIQUE, window_started_at timestamptz NOT NULL,
    request_count integer NOT NULL DEFAULT 0 CHECK (request_count >= 0), blocked_until timestamptz,
    version bigint NOT NULL DEFAULT 0
);
CREATE INDEX ix_api_rate_limit_updated ON api_rate_limit_buckets(updated_at);

CREATE TABLE store_opening_hours (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL, store_id uuid NOT NULL,
    weekday smallint NOT NULL CHECK (weekday BETWEEN 0 AND 6), slot smallint NOT NULL DEFAULT 0 CHECK (slot BETWEEN 0 AND 3),
    opens_at time, closes_at time, closed boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_store_hours_tenant FOREIGN KEY (organization_id, store_id) REFERENCES stores(organization_id,id) ON DELETE RESTRICT,
    CONSTRAINT uk_store_hours_slot UNIQUE (store_id, weekday, slot),
    CONSTRAINT ck_store_hours_range CHECK ((closed AND opens_at IS NULL AND closes_at IS NULL) OR (NOT closed AND opens_at IS NOT NULL AND closes_at IS NOT NULL AND opens_at <> closes_at))
);
CREATE INDEX ix_store_hours_tenant ON store_opening_hours(organization_id, store_id, weekday);

ALTER TABLE products ADD CONSTRAINT uk_products_tenant_id UNIQUE (organization_id,store_id,id);

CREATE TABLE product_addons (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL, store_id uuid NOT NULL, product_id uuid NOT NULL,
    name varchar(100) NOT NULL, description varchar(500), price_delta numeric(12,2) NOT NULL DEFAULT 0 CHECK (price_delta >= 0),
    max_quantity smallint NOT NULL DEFAULT 1 CHECK (max_quantity BETWEEN 1 AND 20), active boolean NOT NULL DEFAULT true,
    archived_at timestamptz, version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_addons_tenant FOREIGN KEY (organization_id,store_id) REFERENCES stores(organization_id,id) ON DELETE RESTRICT,
    CONSTRAINT fk_addons_product_tenant FOREIGN KEY (organization_id,store_id,product_id) REFERENCES products(organization_id,store_id,id) ON DELETE RESTRICT,
    CONSTRAINT uk_product_addons_tenant_id UNIQUE (organization_id,store_id,id),
    CONSTRAINT uk_product_addons_product_ref UNIQUE (organization_id,store_id,product_id,id)
);
CREATE INDEX ix_product_addons_product_active ON product_addons(organization_id,store_id,product_id,active);

CREATE TABLE store_assets (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL, store_id uuid NOT NULL,
    object_key varchar(300) NOT NULL UNIQUE, media_type varchar(80) NOT NULL,
    byte_size bigint NOT NULL CHECK (byte_size BETWEEN 1 AND 10485760),
    sha256 char(64) NOT NULL, width integer, height integer,
    status varchar(16) NOT NULL CHECK (status IN ('PENDING','READY','REJECTED','DELETED')),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_store_assets_tenant FOREIGN KEY (organization_id,store_id) REFERENCES stores(organization_id,id) ON DELETE RESTRICT,
    CONSTRAINT ck_store_asset_dimensions CHECK ((width IS NULL AND height IS NULL) OR (width BETWEEN 1 AND 12000 AND height BETWEEN 1 AND 12000))
);
CREATE INDEX ix_store_assets_tenant_status ON store_assets(organization_id,store_id,status);

CREATE TABLE customer_profiles (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL, store_id uuid NOT NULL,
    display_name varchar(120) NOT NULL, email_normalized varchar(254), phone_e164 varchar(16),
    contact_verified_at timestamptz, marketing_consent_at timestamptz, marketing_consent_version varchar(40),
    status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','BLOCKED','ERASURE_PENDING','ERASED')),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_customer_profiles_tenant FOREIGN KEY (organization_id,store_id) REFERENCES stores(organization_id,id) ON DELETE RESTRICT,
    CONSTRAINT uk_customer_profiles_tenant_id UNIQUE (organization_id,store_id,id),
    CONSTRAINT ck_customer_profile_contact CHECK (email_normalized IS NOT NULL OR phone_e164 IS NOT NULL),
    CONSTRAINT ck_customer_phone CHECK (phone_e164 IS NULL OR phone_e164 ~ '^\\+[1-9][0-9]{7,14}$'),
    CONSTRAINT ck_customer_marketing_consent CHECK ((marketing_consent_at IS NULL) = (marketing_consent_version IS NULL))
);
CREATE INDEX ix_customer_profiles_tenant_email ON customer_profiles(organization_id,store_id,email_normalized) WHERE email_normalized IS NOT NULL;
CREATE INDEX ix_customer_profiles_tenant_phone ON customer_profiles(organization_id,store_id,phone_e164) WHERE phone_e164 IS NOT NULL;

CREATE TABLE customer_addresses (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL, store_id uuid NOT NULL, customer_id uuid NOT NULL,
    label varchar(50), recipient_name varchar(120) NOT NULL, phone_e164 varchar(16),
    postal_code varchar(9), street varchar(180) NOT NULL, street_number varchar(30) NOT NULL,
    address_complement varchar(120), neighborhood varchar(120) NOT NULL, city varchar(120) NOT NULL, state_code varchar(2) NOT NULL,
    latitude numeric(9,6), longitude numeric(9,6), is_default boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_customer_addresses_tenant FOREIGN KEY (organization_id,store_id,customer_id) REFERENCES customer_profiles(organization_id,store_id,id) ON DELETE RESTRICT,
    CONSTRAINT ck_customer_address_state CHECK (state_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_customer_address_location CHECK (((latitude IS NULL) = (longitude IS NULL)) AND (latitude IS NULL OR (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)))
);
CREATE INDEX ix_customer_addresses_customer ON customer_addresses(organization_id,store_id,customer_id);
CREATE UNIQUE INDEX uk_customer_default_address ON customer_addresses(customer_id) WHERE is_default;

CREATE TABLE shopping_carts (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL, store_id uuid NOT NULL, customer_id uuid,
    anonymous_token_hash char(64) UNIQUE, status varchar(16) NOT NULL CHECK (status IN ('ACTIVE','CHECKED_OUT','ABANDONED','EXPIRED')),
    expires_at timestamptz NOT NULL, version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_carts_tenant_id UNIQUE (organization_id,store_id,id),
    CONSTRAINT fk_carts_tenant FOREIGN KEY (organization_id,store_id) REFERENCES stores(organization_id,id) ON DELETE RESTRICT,
    CONSTRAINT fk_carts_customer_tenant FOREIGN KEY (organization_id,store_id,customer_id) REFERENCES customer_profiles(organization_id,store_id,id) ON DELETE RESTRICT,
    CONSTRAINT ck_carts_identity CHECK (customer_id IS NOT NULL OR anonymous_token_hash IS NOT NULL)
);
CREATE INDEX ix_carts_tenant_status_expiry ON shopping_carts(organization_id,store_id,status,expires_at);
CREATE UNIQUE INDEX uk_customer_active_cart ON shopping_carts(customer_id) WHERE customer_id IS NOT NULL AND status='ACTIVE';

CREATE TABLE cart_items (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL, store_id uuid NOT NULL, cart_id uuid NOT NULL, product_id uuid NOT NULL,
    quantity smallint NOT NULL CHECK (quantity BETWEEN 1 AND 99), configuration_hash char(64) NOT NULL,
    customer_note varchar(300), version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_cart_items_cart_tenant FOREIGN KEY (organization_id,store_id,cart_id) REFERENCES shopping_carts(organization_id,store_id,id) ON DELETE RESTRICT,
    CONSTRAINT fk_cart_items_product_tenant FOREIGN KEY (organization_id,store_id,product_id) REFERENCES products(organization_id,store_id,id) ON DELETE RESTRICT,
    CONSTRAINT uk_cart_item_configuration UNIQUE(cart_id,product_id,configuration_hash),
    CONSTRAINT uk_cart_items_product_ref UNIQUE (organization_id,store_id,id,product_id)
);
CREATE INDEX ix_cart_items_tenant_cart ON cart_items(organization_id,store_id,cart_id);

CREATE TABLE cart_item_addons (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL, store_id uuid NOT NULL, cart_item_id uuid NOT NULL, product_id uuid NOT NULL, addon_id uuid NOT NULL,
    quantity smallint NOT NULL CHECK (quantity BETWEEN 1 AND 20),
    CONSTRAINT fk_cart_addon_item_product FOREIGN KEY (organization_id,store_id,cart_item_id,product_id) REFERENCES cart_items(organization_id,store_id,id,product_id) ON DELETE RESTRICT,
    CONSTRAINT fk_cart_addon_catalog_product FOREIGN KEY (organization_id,store_id,product_id,addon_id) REFERENCES product_addons(organization_id,store_id,product_id,id) ON DELETE RESTRICT,
    CONSTRAINT uk_cart_item_addon UNIQUE(cart_item_id,addon_id)
);
CREATE INDEX ix_cart_item_addons_item ON cart_item_addons(organization_id,store_id,cart_item_id);

GRANT DELETE ON TABLE api_rate_limit_buckets TO peditto_app;
