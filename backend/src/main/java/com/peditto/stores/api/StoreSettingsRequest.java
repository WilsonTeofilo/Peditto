package com.peditto.stores.api;

import com.peditto.stores.domain.StorePolicy;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record StoreSettingsRequest(
        @NotBlank @Size(max = StorePolicy.MAX_STORE_NAME_LENGTH) String displayName,
        @NotBlank @Size(max = StorePolicy.MAX_TIMEZONE_LENGTH)
        @Pattern(regexp = "[A-Za-z0-9_+./-]+") String timezone,
        @Size(max = StorePolicy.MAX_STREET_LENGTH) String street,
        @Size(max = StorePolicy.MAX_STREET_NUMBER_LENGTH) String streetNumber,
        @Size(max = StorePolicy.MAX_ADDRESS_COMPLEMENT_LENGTH) String addressComplement,
        @Size(max = StorePolicy.MAX_NEIGHBORHOOD_LENGTH) String neighborhood,
        @Size(max = StorePolicy.MAX_CITY_LENGTH) String city,
        @Size(max = StorePolicy.STATE_CODE_LENGTH) @Pattern(regexp = "([A-Z]{2})?") String stateCode,
        @Size(max = StorePolicy.POSTAL_CODE_LENGTH)
        @Pattern(regexp = "([0-9]{5}-?[0-9]{3})?") String postalCode,
        @DecimalMin(StorePolicy.MIN_LATITUDE) @DecimalMax(StorePolicy.MAX_LATITUDE)
        @Digits(integer = 2, fraction = StorePolicy.COORDINATE_DECIMAL_PLACES) BigDecimal latitude,
        @DecimalMin(StorePolicy.MIN_LONGITUDE) @DecimalMax(StorePolicy.MAX_LONGITUDE)
        @Digits(integer = 3, fraction = StorePolicy.COORDINATE_DECIMAL_PLACES) BigDecimal longitude,
        @NotNull Boolean acceptsPix,
        @NotNull Boolean acceptsCard,
        @NotNull Boolean acceptsCashOnDelivery,
        @NotNull @DecimalMin("0.00")
        @Digits(integer = StorePolicy.DELIVERY_FEE_PRECISION - StorePolicy.DELIVERY_FEE_SCALE,
                fraction = StorePolicy.DELIVERY_FEE_SCALE) BigDecimal deliveryFee,
        @NotNull @DecimalMin("0") @DecimalMax("1440") Integer preparationMinutes) {}
