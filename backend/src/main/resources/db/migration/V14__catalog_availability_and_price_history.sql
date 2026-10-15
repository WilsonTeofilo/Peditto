ALTER TABLE products
    ADD COLUMN available_for_sale boolean NOT NULL DEFAULT true;

CREATE TABLE product_price_history (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL,
    store_id uuid NOT NULL,
    product_id uuid NOT NULL,
    actor_user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    old_price numeric(12,2),
    new_price numeric(12,2) NOT NULL,
    changed_at timestamptz NOT NULL,
    CONSTRAINT fk_product_price_history_product
        FOREIGN KEY (organization_id, store_id, product_id)
        REFERENCES products (organization_id, store_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_product_price_history_old CHECK (old_price IS NULL OR old_price >= 0),
    CONSTRAINT ck_product_price_history_new CHECK (new_price >= 0),
    CONSTRAINT ck_product_price_history_changed CHECK (old_price IS NULL OR old_price <> new_price)
);
CREATE INDEX ix_product_price_history_product_changed
    ON product_price_history (organization_id, store_id, product_id, changed_at DESC);

ALTER TABLE product_price_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_price_history FORCE ROW LEVEL SECURITY;
CREATE POLICY peditto_tenant_isolation ON product_price_history
    FOR ALL TO peditto_app
    USING (organization_id = NULLIF(current_setting('peditto.organization_id', true), '')::uuid)
    WITH CHECK (organization_id = NULLIF(current_setting('peditto.organization_id', true), '')::uuid);
CREATE POLICY peditto_migrator_maintenance ON product_price_history
    FOR ALL TO peditto_migrator USING (true) WITH CHECK (true);
REVOKE UPDATE, DELETE, TRUNCATE ON product_price_history FROM peditto_app;
GRANT SELECT, INSERT ON product_price_history TO peditto_app;

CREATE TRIGGER trg_product_price_history_immutable
    BEFORE UPDATE OR DELETE ON product_price_history
    FOR EACH ROW EXECUTE FUNCTION peditto_reject_immutable_mutation();

COMMENT ON TABLE product_price_history IS
    'Immutable tenant-scoped audit history for initial prices and subsequent changes.';
