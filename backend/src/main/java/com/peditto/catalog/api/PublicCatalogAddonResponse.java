package com.peditto.catalog.api;

import java.math.BigDecimal;
import java.util.UUID;

public record PublicCatalogAddonResponse(
        UUID id, String name, String description, BigDecimal priceDelta, int maxQuantity) {}
