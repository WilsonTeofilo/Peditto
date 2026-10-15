package com.peditto.catalog.api;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductAddonResponse(
        UUID id,
        UUID productId,
        String name,
        String description,
        BigDecimal priceDelta,
        short maxQuantity,
        boolean active,
        long version) {}
