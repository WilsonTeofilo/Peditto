package com.peditto.catalog.infrastructure;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PublicCatalogRepository {
    private final JdbcTemplate jdbc;

    public PublicCatalogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PublicCatalogRow> findByStoreSlug(String slug) {
        return jdbc.query("SELECT * FROM peditto_find_public_catalog_by_slug(?)",
                (result, rowNumber) -> new PublicCatalogRow(
                        result.getObject("product_id", UUID.class),
                        result.getObject("category_id", UUID.class),
                        result.getString("category_name"), result.getObject("category_order", Integer.class),
                        result.getString("product_name"), result.getString("product_description"),
                        result.getBigDecimal("product_price"), result.getObject("addon_id", UUID.class),
                        result.getString("addon_name"), result.getString("addon_description"),
                        result.getBigDecimal("addon_price_delta"), result.getObject("addon_max_quantity", Integer.class)),
                slug);
    }

    public record PublicCatalogRow(UUID productId, UUID categoryId, String categoryName, Integer categoryOrder,
            String productName, String productDescription, BigDecimal productPrice, UUID addonId,
            String addonName, String addonDescription, BigDecimal addonPriceDelta, Integer addonMaxQuantity) {}
}
