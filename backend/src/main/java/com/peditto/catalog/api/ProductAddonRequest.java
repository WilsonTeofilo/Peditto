package com.peditto.catalog.api;

import com.peditto.catalog.domain.CatalogPolicy;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductAddonRequest(
        @NotBlank @Size(max = CatalogPolicy.MAX_ADDON_NAME_LENGTH) String name,
        @Size(max = CatalogPolicy.MAX_ADDON_DESCRIPTION_LENGTH) String description,
        @NotNull @DecimalMin("0.00")
        @Digits(integer = CatalogPolicy.MONEY_PRECISION - CatalogPolicy.MONEY_SCALE,
                fraction = CatalogPolicy.MONEY_SCALE) BigDecimal priceDelta,
        @Min(1) @Max(CatalogPolicy.MAX_ADDON_QUANTITY) short maxQuantity) {}
