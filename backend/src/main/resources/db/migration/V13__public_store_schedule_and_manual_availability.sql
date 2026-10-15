CREATE FUNCTION peditto_find_public_store_by_slug_v2(store_slug text)
RETURNS TABLE (
    slug varchar,
    display_name varchar,
    timezone varchar,
    store_status varchar,
    schedule_open boolean,
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
    SELECT stores.slug,
           stores.display_name,
           stores.timezone,
           stores.status,
           EXISTS (
               SELECT 1
               FROM public.store_opening_hours AS hours
               WHERE hours.organization_id = stores.organization_id
                 AND hours.store_id = stores.id
                 AND hours.weekday = EXTRACT(ISODOW FROM (CURRENT_TIMESTAMP AT TIME ZONE stores.timezone)) - 1
                 AND hours.closed = false
                 AND (CURRENT_TIMESTAMP AT TIME ZONE stores.timezone)::time >= hours.opens_at
                 AND (CURRENT_TIMESTAMP AT TIME ZONE stores.timezone)::time < hours.closes_at
           ),
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
      AND stores.status IN ('ACTIVE', 'CLOSED')
      AND organizations.status = 'ACTIVE'
    LIMIT 1
$$;
ALTER FUNCTION peditto_find_public_store_by_slug_v2(text) OWNER TO peditto_migrator;
REVOKE ALL ON FUNCTION peditto_find_public_store_by_slug_v2(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION peditto_find_public_store_by_slug_v2(text) TO peditto_app;

DROP FUNCTION peditto_find_public_store_by_slug(text);
