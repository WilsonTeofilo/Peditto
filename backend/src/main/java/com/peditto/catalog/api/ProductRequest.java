package com.peditto.catalog.api;

import com.peditto.catalog.domain.CatalogPolicy;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotBlank @Size(max = CatalogPolicy.MAX_PRODUCT_NAME_LENGTH) String name,
        @Size(max = CatalogPolicy.MAX_PRODUCT_DESCRIPTION_LENGTH) String description,
        @NotNull @DecimalMin("0.00")
        @Digits(integer = CatalogPolicy.MONEY_PRECISION - CatalogPolicy.MONEY_SCALE,
                fraction = CatalogPolicy.MONEY_SCALE) BigDecimal price,
        UUID categoryId) {}
