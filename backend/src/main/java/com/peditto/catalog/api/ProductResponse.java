package com.peditto.catalog.api;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID categoryId,
        String name,
        String description,
        BigDecimal price,
        boolean active,
        boolean availableForSale,
        long version) {}
