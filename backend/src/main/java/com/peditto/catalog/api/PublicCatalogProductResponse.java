package com.peditto.catalog.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PublicCatalogProductResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        Integer categoryOrder,
        String name,
        String description,
        BigDecimal price,
        List<PublicCatalogAddonResponse> addons) {
    public PublicCatalogProductResponse {
        addons = List.copyOf(addons);
    }
}
