package com.peditto.stores.api;

import java.math.BigDecimal;

public record PublicStoreResponse(
        String slug,
        String displayName,
        String timezone,
        boolean openNow,
        BigDecimal deliveryFee,
        BigDecimal deliveryCenterLatitude,
        BigDecimal deliveryCenterLongitude,
        Integer deliveryRadiusMeters,
        boolean deliveryAvailable) {}
