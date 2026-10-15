package com.peditto.stores.api;

import java.math.BigDecimal;
import java.util.UUID;

public record StoreSettingsResponse(
        UUID id,
        String slug,
        String displayName,
        String status,
        String timezone,
        String street,
        String streetNumber,
        String addressComplement,
        String neighborhood,
        String city,
        String stateCode,
        String postalCode,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean acceptsPix,
        boolean acceptsCard,
        boolean acceptsCashOnDelivery,
        BigDecimal deliveryFee,
        int preparationMinutes,
        long version) {}
