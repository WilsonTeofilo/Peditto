package com.peditto.stores.api;

import java.time.LocalTime;
import java.util.List;

public record OpeningHoursResponse(List<Interval> intervals) {
    public record Interval(short weekday, short slot, LocalTime opensAt, LocalTime closesAt) {}
}
