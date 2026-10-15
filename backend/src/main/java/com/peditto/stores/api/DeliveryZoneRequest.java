package com.peditto.stores.api;

import com.peditto.stores.domain.StorePolicy;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DeliveryZoneRequest(
        @NotNull @DecimalMin(StorePolicy.MIN_LATITUDE) @DecimalMax(StorePolicy.MAX_LATITUDE)
        @Digits(integer = 2, fraction = StorePolicy.COORDINATE_DECIMAL_PLACES) BigDecimal centerLatitude,
        @NotNull @DecimalMin(StorePolicy.MIN_LONGITUDE) @DecimalMax(StorePolicy.MAX_LONGITUDE)
        @Digits(integer = 3, fraction = StorePolicy.COORDINATE_DECIMAL_PLACES) BigDecimal centerLongitude,
        @Min(StorePolicy.MIN_DELIVERY_RADIUS_METERS)
        @Max(StorePolicy.MAX_DELIVERY_RADIUS_METERS) int radiusMeters) {}
