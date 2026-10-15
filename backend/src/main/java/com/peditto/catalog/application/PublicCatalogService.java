package com.peditto.catalog.application;

import com.peditto.catalog.api.PublicCatalogAddonResponse;
import com.peditto.catalog.api.PublicCatalogProductResponse;
import com.peditto.catalog.infrastructure.PublicCatalogRepository;
import com.peditto.catalog.infrastructure.PublicCatalogRepository.PublicCatalogRow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicCatalogService {
    private final PublicCatalogRepository catalog;

    public PublicCatalogService(PublicCatalogRepository catalog) {
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public List<PublicCatalogProductResponse> findByStoreSlug(String slug) {
        if (slug == null || !slug.matches("[a-z0-9](?:[a-z0-9-]{0,58}[a-z0-9])?")) {
            return List.of();
        }

        Map<java.util.UUID, ProductBuilder> products = new LinkedHashMap<>();
        for (PublicCatalogRow row : catalog.findByStoreSlug(slug.toLowerCase(Locale.ROOT))) {
            ProductBuilder product = products.computeIfAbsent(row.productId(), ignored -> new ProductBuilder(row));
            if (row.addonId() != null) {
                product.addons.add(new PublicCatalogAddonResponse(row.addonId(), row.addonName(),
                        row.addonDescription(), row.addonPriceDelta(), row.addonMaxQuantity()));
            }
        }
        return products.values().stream().map(ProductBuilder::build).toList();
    }

    private static final class ProductBuilder {
        private final PublicCatalogRow row;
        private final List<PublicCatalogAddonResponse> addons = new ArrayList<>();

        private ProductBuilder(PublicCatalogRow row) { this.row = row; }

        private PublicCatalogProductResponse build() {
            return new PublicCatalogProductResponse(row.productId(), row.categoryId(), row.categoryName(),
                    row.categoryOrder(), row.productName(), row.productDescription(), row.productPrice(), addons);
        }
    }
}
