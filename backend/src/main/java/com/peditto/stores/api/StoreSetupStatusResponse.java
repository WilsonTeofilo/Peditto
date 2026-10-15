package com.peditto.stores.api;

public record StoreSetupStatusResponse(
        boolean profileConfigured,
        boolean addressConfigured,
        boolean deliveryZoneConfigured,
        boolean deliveryZonePublished,
        boolean openingHoursConfigured,
        boolean readyToPublish) {}
