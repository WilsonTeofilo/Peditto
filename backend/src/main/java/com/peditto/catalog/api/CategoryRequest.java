package com.peditto.catalog.api;

import com.peditto.catalog.domain.CatalogPolicy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = CatalogPolicy.MAX_CATEGORY_NAME_LENGTH) String name,
        @NotNull @Min(0) Integer displayOrder) {}
