package com.peditto.stores.api;

import com.peditto.stores.application.StoreSetupService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stores/current")
public class StoreSetupController {
    private final StoreSetupService setup;

    public StoreSetupController(StoreSetupService setup) {
        this.setup = setup;
    }

    @GetMapping("/settings")
    @PreAuthorize("@tenantAuthorization.hasCapability('settings:read')")
    public StoreSettingsResponse settings() {
        return setup.getSettings();
    }

    @GetMapping("/setup-status")
    @PreAuthorize("@tenantAuthorization.hasCapability('settings:read')")
    public StoreSetupStatusResponse setupStatus() {
        return setup.getSetupStatus();
    }

    @PutMapping("/settings")
    @PreAuthorize("@tenantAuthorization.hasCapability('settings:write')")
    public StoreSettingsResponse updateSettings(@Valid @RequestBody StoreSettingsRequest request) {
        return setup.updateSettings(request);
    }

    @GetMapping("/delivery-zone")
    @PreAuthorize("@tenantAuthorization.hasCapability('settings:read')")
    public DeliveryZoneResponse deliveryZone() {
        return setup.getDeliveryZone();
    }

    @PutMapping("/delivery-zone")
    @PreAuthorize("@tenantAuthorization.hasCapability('settings:write')")
    public DeliveryZoneResponse updateDeliveryZone(@Valid @RequestBody DeliveryZoneRequest request) {
        return setup.updateDeliveryZone(request);
    }

    @PostMapping("/delivery-zone/publish")
    @PreAuthorize("@tenantAuthorization.hasCapability('settings:write')")
    public DeliveryZoneResponse publishDeliveryZone() {
        return setup.publishDeliveryZone();
    }

    @GetMapping("/opening-hours")
    @PreAuthorize("@tenantAuthorization.hasCapability('settings:read')")
    public OpeningHoursResponse openingHours() {
        return setup.getOpeningHours();
    }

    @PutMapping("/opening-hours")
    @PreAuthorize("@tenantAuthorization.hasCapability('settings:write')")
    public OpeningHoursResponse updateOpeningHours(@Valid @RequestBody OpeningHoursRequest request) {
        return setup.updateOpeningHours(request);
    }

    @PostMapping("/publish")
    @PreAuthorize("@tenantAuthorization.hasCapability('settings:write')")
    public StoreSettingsResponse publish() {
        return setup.publishStore();
    }

    @PostMapping("/availability/close")
    @PreAuthorize("@tenantAuthorization.hasCapability('settings:write')")
    public StoreSettingsResponse close() {
        return setup.closeStore();
    }

    @PostMapping("/availability/reopen")
    @PreAuthorize("@tenantAuthorization.hasCapability('settings:write')")
    public StoreSettingsResponse reopen() {
        return setup.reopenStore();
    }
}
