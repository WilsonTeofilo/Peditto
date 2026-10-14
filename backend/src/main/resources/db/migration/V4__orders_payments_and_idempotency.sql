CREATE TABLE customer_orders (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL, store_id uuid NOT NULL, customer_id uuid,
    order_number varchar(20) NOT NULL,
    status varchar(24) NOT NULL CHECK (status IN ('PENDING_PAYMENT','PLACED','ACCEPTED','PREPARING','READY','DISPATCHED','DELIVERED','COMPLETED','CANCELLED','REFUNDED')),
    payment_status varchar(20) NOT NULL CHECK (payment_status IN ('PENDING','AUTHORIZED','PAID','FAILED','REFUNDED','PARTIALLY_REFUNDED')),
    fulfillment_type varchar(16) NOT NULL CHECK (fulfillment_type IN ('DELIVERY','PICKUP','DINE_IN')),
    subtotal numeric(12,2) NOT NULL CHECK (subtotal >= 0), delivery_fee numeric(12,2) NOT NULL DEFAULT 0 CHECK (delivery_fee >= 0),
    discount_amount numeric(12,2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0), total_amount numeric(12,2) NOT NULL CHECK (total_amount >= 0),
    currency char(3) NOT NULL DEFAULT 'BRL' CHECK (currency='BRL'),
    customer_name_snapshot varchar(120) NOT NULL, customer_phone_snapshot varchar(16) NOT NULL,
    shipping_address_snapshot jsonb, delivery_latitude numeric(9,6), delivery_longitude numeric(9,6),
    customer_note varchar(500), placed_at timestamptz, cancelled_at timestamptz, cancel_reason_code varchar(60),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_orders_store_tenant FOREIGN KEY (organization_id,store_id) REFERENCES stores(organization_id,id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_customer_tenant FOREIGN KEY (organization_id,store_id,customer_id) REFERENCES customer_profiles(organization_id,store_id,id) ON DELETE RESTRICT,
    CONSTRAINT uk_orders_tenant_id UNIQUE (organization_id,store_id,id),
    CONSTRAINT uk_orders_number UNIQUE(store_id,order_number),
    CONSTRAINT ck_order_total CHECK (total_amount = subtotal + delivery_fee - discount_amount),
    CONSTRAINT ck_order_delivery_location CHECK (((delivery_latitude IS NULL) = (delivery_longitude IS NULL)) AND (delivery_latitude IS NULL OR (delivery_latitude BETWEEN -90 AND 90 AND delivery_longitude BETWEEN -180 AND 180)))
);
CREATE INDEX ix_orders_tenant_status_created ON customer_orders(organization_id,store_id,status,created_at DESC);
CREATE INDEX ix_orders_customer_created ON customer_orders(customer_id,created_at DESC);

CREATE TABLE order_items (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL, store_id uuid NOT NULL, order_id uuid NOT NULL,
    product_id uuid, product_name_snapshot varchar(140) NOT NULL, description_snapshot varchar(2000),
    quantity smallint NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    unit_price_snapshot numeric(12,2) NOT NULL CHECK (unit_price_snapshot >= 0),
    unit_cost_snapshot numeric(12,2) CHECK (unit_cost_snapshot IS NULL OR unit_cost_snapshot >= 0),
    line_total numeric(12,2) NOT NULL CHECK (line_total >= 0), customer_note varchar(300), version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_order_items_order_tenant FOREIGN KEY (organization_id,store_id,order_id) REFERENCES customer_orders(organization_id,store_id,id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_items_product_tenant FOREIGN KEY (organization_id,store_id,product_id) REFERENCES products(organization_id,store_id,id) ON DELETE RESTRICT,
    CONSTRAINT uk_order_items_tenant_id UNIQUE (organization_id,store_id,id),
    CONSTRAINT uk_order_items_product_ref UNIQUE (organization_id,store_id,id,product_id),
    CONSTRAINT ck_order_line_total CHECK (line_total = unit_price_snapshot * quantity)
);
CREATE INDEX ix_order_items_tenant_order ON order_items(organization_id,store_id,order_id);

CREATE TABLE order_item_addons (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL, store_id uuid NOT NULL, order_item_id uuid NOT NULL, product_id uuid,
    addon_id uuid, addon_name_snapshot varchar(100) NOT NULL, quantity smallint NOT NULL CHECK (quantity BETWEEN 1 AND 20),
    unit_price_snapshot numeric(12,2) NOT NULL CHECK (unit_price_snapshot >= 0),
    CONSTRAINT fk_order_item_addons_item FOREIGN KEY (organization_id,store_id,order_item_id,product_id) REFERENCES order_items(organization_id,store_id,id,product_id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_item_addons_catalog FOREIGN KEY (organization_id,store_id,product_id,addon_id) REFERENCES product_addons(organization_id,store_id,product_id,id) ON DELETE RESTRICT
);
CREATE INDEX ix_order_item_addons_item ON order_item_addons(organization_id,store_id,order_item_id);

CREATE TABLE order_events (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL, store_id uuid NOT NULL, order_id uuid NOT NULL,
    actor_user_id uuid REFERENCES app_users(id) ON DELETE SET NULL,
    from_status varchar(24), to_status varchar(24) NOT NULL, reason_code varchar(60), request_id varchar(80), metadata_redacted jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT fk_order_events_order_tenant FOREIGN KEY (organization_id,store_id,order_id) REFERENCES customer_orders(organization_id,store_id,id) ON DELETE RESTRICT
);
CREATE INDEX ix_order_events_order_time ON order_events(organization_id,store_id,order_id,created_at);

CREATE TABLE order_payments (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL, store_id uuid NOT NULL, order_id uuid NOT NULL,
    method varchar(24) NOT NULL CHECK (method IN ('PIX','CARD','CASH_ON_DELIVERY')),
    status varchar(20) NOT NULL CHECK (status IN ('PENDING','AUTHORIZED','PAID','FAILED','CANCELLED','REFUNDED','PARTIALLY_REFUNDED')),
    amount numeric(12,2) NOT NULL CHECK (amount >= 0), currency char(3) NOT NULL DEFAULT 'BRL' CHECK (currency='BRL'),
    provider varchar(40), provider_payment_reference varchar(180), provider_idempotency_key varchar(160),
    cash_change_for numeric(12,2) CHECK (cash_change_for IS NULL OR cash_change_for >= amount),
    authorized_at timestamptz, paid_at timestamptz, refunded_amount numeric(12,2) NOT NULL DEFAULT 0 CHECK (refunded_amount BETWEEN 0 AND amount),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_order_payments_order_tenant FOREIGN KEY (organization_id,store_id,order_id) REFERENCES customer_orders(organization_id,store_id,id) ON DELETE RESTRICT,
    CONSTRAINT ck_order_cash_fields CHECK ((method='CASH_ON_DELIVERY') OR cash_change_for IS NULL),
    CONSTRAINT ck_order_provider_fields CHECK ((method='CASH_ON_DELIVERY') OR (provider IS NOT NULL AND provider_payment_reference IS NOT NULL))
);
CREATE UNIQUE INDEX uk_order_payment_provider_ref ON order_payments(provider,provider_payment_reference) WHERE provider_payment_reference IS NOT NULL;
CREATE UNIQUE INDEX uk_order_payment_provider_idempotency ON order_payments(provider,provider_idempotency_key) WHERE provider_idempotency_key IS NOT NULL;
CREATE INDEX ix_order_payments_order ON order_payments(organization_id,store_id,order_id,status);

CREATE TABLE idempotency_records (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    actor_fingerprint char(64) NOT NULL, operation varchar(100) NOT NULL, idempotency_key varchar(128) NOT NULL,
    request_fingerprint char(64) NOT NULL, state varchar(16) NOT NULL CHECK (state IN ('PROCESSING','COMPLETED','FAILED')),
    resource_type varchar(80), resource_id uuid, response_status smallint, expires_at timestamptz NOT NULL,
    CONSTRAINT uk_idempotency_scope UNIQUE (organization_id,actor_fingerprint,operation,idempotency_key),
    CONSTRAINT ck_idempotency_result CHECK ((state <> 'COMPLETED') OR (response_status IS NOT NULL))
);
CREATE INDEX ix_idempotency_expiry ON idempotency_records(expires_at);

CREATE OR REPLACE FUNCTION peditto_reject_immutable_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'immutable record';
END;
$$;
CREATE TRIGGER trg_order_events_immutable BEFORE UPDATE OR DELETE ON order_events FOR EACH ROW EXECUTE FUNCTION peditto_reject_immutable_mutation();
CREATE TRIGGER trg_audit_events_immutable BEFORE UPDATE OR DELETE ON audit_events FOR EACH ROW EXECUTE FUNCTION peditto_reject_immutable_mutation();
