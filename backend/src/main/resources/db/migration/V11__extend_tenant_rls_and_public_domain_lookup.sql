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

ALTER TABLE store_domains ADD COLUMN organization_id uuid;
UPDATE store_domains AS domains
SET organization_id = stores.organization_id
FROM stores
WHERE stores.id = domains.store_id;
ALTER TABLE store_domains ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE store_domains
    ADD CONSTRAINT fk_store_domains_store_tenant
        FOREIGN KEY (organization_id, store_id) REFERENCES stores (organization_id, id) ON DELETE RESTRICT;
CREATE INDEX ix_store_domains_tenant_store ON store_domains (organization_id, store_id);

DO $$
DECLARE
    tenant_table text;
    tenant_tables text[] := ARRAY[
        'stores', 'store_settings', 'delivery_zones', 'store_opening_hours', 'store_domains',
        'trials', 'billing_invoices', 'organization_subscriptions', 'invoice_payments',
        'financial_ledger_entries', 'idempotency_records'
    ];
BEGIN
    FOREACH tenant_table IN ARRAY tenant_tables LOOP
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns AS columns_in_schema
            WHERE columns_in_schema.table_schema = current_schema()
              AND columns_in_schema.table_name = tenant_table
              AND columns_in_schema.column_name = 'organization_id'
        ) THEN
            RAISE EXCEPTION 'Cannot enable tenant RLS: %.organization_id is missing', tenant_table;
        END IF;

        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', tenant_table);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', tenant_table);
        EXECUTE format(
            'CREATE POLICY peditto_tenant_isolation ON %I FOR ALL TO peditto_app USING (organization_id = NULLIF(current_setting(''peditto.organization_id'', true), '''')::uuid) WITH CHECK (organization_id = NULLIF(current_setting(''peditto.organization_id'', true), '''')::uuid)',
            tenant_table);
        EXECUTE format(
            'CREATE POLICY peditto_migrator_maintenance ON %I FOR ALL TO peditto_migrator USING (true) WITH CHECK (true)',
            tenant_table);
    END LOOP;
END
$$;

-- This function is the only cross-tenant read path for public custom-domain resolution.
-- Its owner can see rows through the explicit migrator policy; the application role gets EXECUTE only.
CREATE FUNCTION peditto_find_public_store_by_domain(hostname text)
RETURNS TABLE (store_id uuid, slug varchar, display_name varchar, timezone varchar)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT stores.id, stores.slug, stores.display_name, stores.timezone
    FROM public.store_domains AS domains
    JOIN public.stores AS stores
      ON stores.id = domains.store_id
     AND stores.organization_id = domains.organization_id
    JOIN public.organizations AS organizations
      ON organizations.id = stores.organization_id
    WHERE domains.hostname_normalized = lower(btrim(hostname))
      AND domains.active = true
      AND domains.verified_at IS NOT NULL
      AND stores.status = 'ACTIVE'
      AND organizations.status = 'ACTIVE'
    LIMIT 1
$$;
ALTER FUNCTION peditto_find_public_store_by_domain(text) OWNER TO peditto_migrator;
REVOKE ALL ON FUNCTION peditto_find_public_store_by_domain(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION peditto_find_public_store_by_domain(text) TO peditto_app;
