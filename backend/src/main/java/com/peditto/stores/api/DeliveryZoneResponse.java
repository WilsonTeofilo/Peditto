package com.peditto.stores.api;

import java.math.BigDecimal;

public record DeliveryZoneResponse(
        BigDecimal centerLatitude,
        BigDecimal centerLongitude,
        int radiusMeters,
        boolean active,
        int policyVersion,
        long version) {}
