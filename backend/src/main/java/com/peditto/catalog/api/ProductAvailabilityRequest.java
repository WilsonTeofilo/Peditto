package com.peditto.catalog.api;

import jakarta.validation.constraints.NotNull;

public record ProductAvailabilityRequest(@NotNull Boolean available) {}
