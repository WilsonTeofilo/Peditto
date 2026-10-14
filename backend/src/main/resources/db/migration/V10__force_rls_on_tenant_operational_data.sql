DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'peditto_app') THEN
        RAISE EXCEPTION 'Required runtime role peditto_app does not exist';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'peditto_migrator') THEN
        RAISE EXCEPTION 'Required migration role peditto_migrator does not exist';
    END IF;
END
$$;

DO $$
DECLARE
    table_name text;
    tenant_tables text[] := ARRAY[
        'catalog_categories', 'products', 'product_addons', 'store_assets',
        'customer_profiles', 'customer_addresses', 'shopping_carts', 'cart_items',
        'cart_item_addons', 'customer_orders', 'order_items', 'order_item_addons',
        'order_events', 'order_payments'
    ];
BEGIN
    FOREACH table_name IN ARRAY tenant_tables LOOP
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND information_schema.columns.table_name = table_name
              AND column_name = 'organization_id'
        ) THEN
            RAISE EXCEPTION 'Cannot enable tenant RLS: %.organization_id is missing', table_name;
        END IF;

        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', table_name);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', table_name);
        EXECUTE format('CREATE POLICY peditto_tenant_isolation ON %I FOR ALL TO peditto_app USING (organization_id = NULLIF(current_setting(''peditto.organization_id'', true), '''')::uuid) WITH CHECK (organization_id = NULLIF(current_setting(''peditto.organization_id'', true), '''')::uuid)', table_name);
        EXECUTE format('CREATE POLICY peditto_migrator_maintenance ON %I FOR ALL TO peditto_migrator USING (true) WITH CHECK (true)', table_name);
    END LOOP;
END
$$;

COMMENT ON POLICY peditto_tenant_isolation ON products IS
    'Defense in depth: tenant context is set per transaction after server-side membership validation.';
