package com.peditto.stores.api;

import com.peditto.stores.domain.StorePolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;

public record OpeningHoursRequest(
        @NotNull @Size(max = StorePolicy.MAX_INTERVALS_PER_WEEK) List<@Valid Interval> intervals) {
    public record Interval(
            @Min(0) @Max(StorePolicy.DAYS_PER_WEEK - 1) short weekday,
            @NotNull LocalTime opensAt,
            @NotNull LocalTime closesAt) {}
}
