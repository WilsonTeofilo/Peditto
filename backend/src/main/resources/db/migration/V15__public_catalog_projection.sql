CREATE FUNCTION peditto_find_public_catalog_by_slug(store_slug text)
RETURNS TABLE (
    product_id uuid,
    category_id uuid,
    category_name varchar,
    category_order integer,
    product_name varchar,
    product_description varchar,
    product_price numeric,
    addon_id uuid,
    addon_name varchar,
    addon_description varchar,
    addon_price_delta numeric,
    addon_max_quantity integer
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
    SELECT product.id,
           category.id,
           category.name,
           category.display_order,
           product.name,
           product.description,
           product.price,
           addon.id,
           addon.name,
           addon.description,
           addon.price_delta,
           addon.max_quantity::integer
    FROM public.stores AS store
    JOIN public.organizations AS organization
      ON organization.id = store.organization_id
     AND organization.status = 'ACTIVE'
    JOIN public.products AS product
      ON product.organization_id = store.organization_id
     AND product.store_id = store.id
     AND product.active = true
     AND product.available_for_sale = true
    LEFT JOIN public.catalog_categories AS category
      ON category.organization_id = product.organization_id
     AND category.store_id = product.store_id
     AND category.id = product.category_id
     AND category.active = true
    LEFT JOIN public.product_addons AS addon
      ON addon.organization_id = product.organization_id
     AND addon.store_id = product.store_id
     AND addon.product_id = product.id
     AND addon.active = true
    WHERE store.slug = lower(btrim(store_slug))
      AND store.status IN ('ACTIVE', 'CLOSED')
      AND (product.category_id IS NULL OR category.id IS NOT NULL)
    ORDER BY category.display_order NULLS LAST, category.id, product.name, product.id, addon.name, addon.id
$$;
ALTER FUNCTION peditto_find_public_catalog_by_slug(text) OWNER TO peditto_migrator;
REVOKE ALL ON FUNCTION peditto_find_public_catalog_by_slug(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION peditto_find_public_catalog_by_slug(text) TO peditto_app;
