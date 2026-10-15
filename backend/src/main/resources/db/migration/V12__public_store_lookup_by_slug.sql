CREATE FUNCTION peditto_find_public_store_by_slug(store_slug text)
RETURNS TABLE (
    store_id uuid,
    slug varchar,
    display_name varchar,
    timezone varchar,
    delivery_fee numeric,
    center_latitude numeric,
    center_longitude numeric,
    radius_meters integer,
    delivery_active boolean
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT stores.id,
           stores.slug,
           stores.display_name,
           stores.timezone,
           settings.delivery_fee,
           zones.center_latitude,
           zones.center_longitude,
           zones.radius_meters,
           COALESCE(zones.active, false)
    FROM public.stores AS stores
    JOIN public.organizations AS organizations
      ON organizations.id = stores.organization_id
    LEFT JOIN public.store_settings AS settings
      ON settings.organization_id = stores.organization_id
     AND settings.store_id = stores.id
    LEFT JOIN public.delivery_zones AS zones
      ON zones.organization_id = stores.organization_id
     AND zones.store_id = stores.id
    WHERE stores.slug = lower(btrim(store_slug))
      AND stores.status = 'ACTIVE'
      AND organizations.status = 'ACTIVE'
    LIMIT 1
$$;
ALTER FUNCTION peditto_find_public_store_by_slug(text) OWNER TO peditto_migrator;
REVOKE ALL ON FUNCTION peditto_find_public_store_by_slug(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION peditto_find_public_store_by_slug(text) TO peditto_app;
