ALTER TABLE store_assets ADD COLUMN product_id uuid;
ALTER TABLE store_assets
    ADD CONSTRAINT fk_store_assets_product_tenant
    FOREIGN KEY (organization_id, store_id, product_id)
    REFERENCES products (organization_id, store_id, id) ON DELETE RESTRICT;
CREATE UNIQUE INDEX uk_store_assets_product_live
    ON store_assets (organization_id, store_id, product_id)
    WHERE product_id IS NOT NULL AND status <> 'DELETED';
