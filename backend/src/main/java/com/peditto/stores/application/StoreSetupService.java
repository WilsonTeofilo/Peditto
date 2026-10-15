package com.peditto.stores.application;

import com.peditto.common.persistence.AuditWriter;
import com.peditto.common.tenant.TenantContext;
import com.peditto.organizations.domain.Organization;
import com.peditto.stores.api.DeliveryZoneRequest;
import com.peditto.stores.api.DeliveryZoneResponse;
import com.peditto.stores.api.OpeningHoursRequest;
import com.peditto.stores.api.OpeningHoursResponse;
import com.peditto.stores.api.StoreSettingsRequest;
import com.peditto.stores.api.StoreSettingsResponse;
import com.peditto.stores.api.StoreSetupStatusResponse;
import com.peditto.stores.domain.DeliveryZone;
import com.peditto.stores.domain.Store;
import com.peditto.stores.domain.StoreOpeningHour;
import com.peditto.stores.domain.StorePolicy;
import com.peditto.stores.domain.StoreSettings;
import com.peditto.stores.infrastructure.DeliveryZoneRepository;
import com.peditto.stores.infrastructure.StoreOpeningHourRepository;
import com.peditto.stores.infrastructure.StoreRepository;
import com.peditto.stores.infrastructure.StoreSettingsRepository;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StoreSetupService {
    private final StoreRepository stores;
    private final StoreSettingsRepository settings;
    private final DeliveryZoneRepository deliveryZones;
    private final StoreOpeningHourRepository openingHours;
    private final AuditWriter audit;

    public StoreSetupService(StoreRepository stores, StoreSettingsRepository settings,
            DeliveryZoneRepository deliveryZones, StoreOpeningHourRepository openingHours, AuditWriter audit) {
        this.stores = stores;
        this.settings = settings;
        this.deliveryZones = deliveryZones;
        this.openingHours = openingHours;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public StoreSettingsResponse getSettings() {
        Store store = currentStore();
        return response(store, currentSettings(store));
    }

    @Transactional(readOnly = true)
    public StoreSetupStatusResponse getSetupStatus() {
        Store store = currentStore();
        StoreSettings current = currentSettings(store);
        boolean profileConfigured = store.getDisplayName() != null && !store.getDisplayName().isBlank()
                && store.getTimezone() != null && !store.getTimezone().isBlank();
        boolean addressConfigured = hasCompleteAddress(current);
        Optional<DeliveryZone> zone = deliveryZones
                .findByOrganization_IdAndStore_Id(organizationId(), store.getId());
        boolean hoursConfigured = !openingHours.findAllByOrganization_IdAndStore_IdOrderByWeekdayAscSlotAsc(
                organizationId(), store.getId()).isEmpty();
        boolean zonePublished = zone.filter(DeliveryZone::isActive).isPresent();
        return new StoreSetupStatusResponse(profileConfigured, addressConfigured, zone.isPresent(),
                zonePublished, hoursConfigured,
                profileConfigured && addressConfigured && zonePublished && hoursConfigured);
    }

    @Transactional
    public StoreSettingsResponse updateSettings(StoreSettingsRequest request) {
        Store store = currentStore();
        StoreSettings current = currentSettings(store);
        ZoneId timezone = parseTimezone(request.timezone());
        store.updateProfile(request.displayName(), timezone);
        current.replaceAddressAndPayments(request.street(), request.streetNumber(), request.addressComplement(),
                request.neighborhood(), request.city(), request.stateCode(), request.postalCode(),
                request.latitude(), request.longitude(), request.acceptsPix(), request.acceptsCard(),
                request.acceptsCashOnDelivery(), request.deliveryFee(), request.preparationMinutes());
        stores.saveAndFlush(store);
        StoreSettings saved = settings.saveAndFlush(current);
        record("STORE_SETTINGS_UPDATED", store);
        return response(store, saved);
    }

    @Transactional(readOnly = true)
    public DeliveryZoneResponse getDeliveryZone() {
        Store store = currentStore();
        return deliveryZones.findByOrganization_IdAndStore_Id(organizationId(), store.getId())
                .map(this::response)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public DeliveryZoneResponse updateDeliveryZone(DeliveryZoneRequest request) {
        Store store = currentStore();
        Organization organization = store.getOrganization();
        Optional<DeliveryZone> existing = deliveryZones
                .findByOrganization_IdAndStore_Id(organizationId(), store.getId());
        DeliveryZone zone = existing.orElseGet(() -> new DeliveryZone(organization, store,
                request.centerLatitude(), request.centerLongitude(), request.radiusMeters()));
        existing.ifPresent(value -> value.changeCoverage(request.centerLatitude(),
                request.centerLongitude(), request.radiusMeters()));
        DeliveryZone saved = deliveryZones.saveAndFlush(zone);
        record("DELIVERY_ZONE_UPDATED", store);
        return response(saved);
    }

    @Transactional
    public DeliveryZoneResponse publishDeliveryZone() {
        Store store = currentStore();
        DeliveryZone zone = deliveryZones.findByOrganization_IdAndStore_Id(organizationId(), store.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        zone.publish();
        DeliveryZone saved = deliveryZones.saveAndFlush(zone);
        record("DELIVERY_ZONE_PUBLISHED", store);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public OpeningHoursResponse getOpeningHours() {
        Store store = currentStore();
        List<OpeningHoursResponse.Interval> intervals = openingHours
                .findAllByOrganization_IdAndStore_IdOrderByWeekdayAscSlotAsc(organizationId(), store.getId())
                .stream()
                .map(hour -> new OpeningHoursResponse.Interval(hour.getWeekday(), hour.getSlot(),
                        hour.getOpensAt(), hour.getClosesAt()))
                .toList();
        return new OpeningHoursResponse(intervals);
    }

    @Transactional
    public OpeningHoursResponse updateOpeningHours(OpeningHoursRequest request) {
        Store store = currentStore();
        UUID organizationId = organizationId();
        List<OpeningHoursRequest.Interval> sorted = request.intervals().stream()
                .sorted(Comparator.comparingInt((OpeningHoursRequest.Interval interval) -> interval.weekday())
                        .thenComparing(OpeningHoursRequest.Interval::opensAt))
                .toList();
        List<StoreOpeningHour> replacements = new ArrayList<>(sorted.size());
        short currentDay = -1;
        LocalTime previousClose = null;
        short slot = 0;
        for (OpeningHoursRequest.Interval interval : sorted) {
            if (interval.weekday() != currentDay) {
                currentDay = interval.weekday();
                previousClose = null;
                slot = 0;
            }
            if (slot == StorePolicy.MAX_INTERVALS_PER_DAY
                    || (previousClose != null && interval.opensAt().isBefore(previousClose))) {
                throw new IllegalArgumentException("Horários do mesmo dia excedem o limite ou se sobrepõem.");
            }
            replacements.add(new StoreOpeningHour(store.getOrganization(), store, currentDay, slot++,
                    interval.opensAt(), interval.closesAt()));
            previousClose = interval.closesAt();
        }
        openingHours.deleteForStore(organizationId, store.getId());
        List<StoreOpeningHour> saved = openingHours.saveAllAndFlush(replacements);
        record("STORE_HOURS_UPDATED", store);
        return new OpeningHoursResponse(saved.stream()
                .map(hour -> new OpeningHoursResponse.Interval(hour.getWeekday(), hour.getSlot(),
                        hour.getOpensAt(), hour.getClosesAt()))
                .toList());
    }

    @Transactional
    public StoreSettingsResponse publishStore() {
        Store store = currentStore();
        StoreSettings current = currentSettings(store);
        ensurePublishReady(store, current);
        store.publish();
        Store saved = stores.saveAndFlush(store);
        record("STORE_PUBLISHED", saved);
        return response(saved, current);
    }

    @Transactional
    public StoreSettingsResponse closeStore() {
        Store store = currentStore();
        store.closeForOrders();
        Store saved = stores.saveAndFlush(store);
        record("STORE_CLOSED_FOR_ORDERS", saved);
        return response(saved, currentSettings(saved));
    }

    @Transactional
    public StoreSettingsResponse reopenStore() {
        Store store = currentStore();
        ensurePublishReady(store, currentSettings(store));
        store.reopenForOrders();
        Store saved = stores.saveAndFlush(store);
        record("STORE_REOPENED_FOR_ORDERS", saved);
        return response(saved, currentSettings(saved));
    }

    private void ensurePublishReady(Store store, StoreSettings current) {
        boolean addressComplete = hasCompleteAddress(current);
        boolean zoneActive = deliveryZones.findByOrganization_IdAndStore_Id(
                organizationId(), store.getId()).filter(DeliveryZone::isActive).isPresent();
        boolean hasOpeningHours = !openingHours.findAllByOrganization_IdAndStore_IdOrderByWeekdayAscSlotAsc(
                organizationId(), store.getId()).isEmpty();
        if (!addressComplete || !zoneActive || !hasOpeningHours) {
            throw new IllegalStateException("Complete endereço, zona de entrega e horários antes de abrir a loja.");
        }
    }

    private static boolean hasCompleteAddress(StoreSettings settings) {
        return settings.getStreet() != null && settings.getCity() != null
                && settings.getStateCode() != null && settings.getPostalCode() != null
                && settings.getLatitude() != null && settings.getLongitude() != null;
    }

    private Store currentStore() {
        return stores.findByOrganization_Id(organizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private StoreSettings currentSettings(Store store) {
        return settings.findByOrganization_IdAndStore_Id(organizationId(), store.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private UUID organizationId() {
        return TenantContext.requireOrganizationId();
    }

    private StoreSettingsResponse response(Store store, StoreSettings value) {
        return new StoreSettingsResponse(store.getId(), store.getSlug(), store.getDisplayName(),
                store.getStatus().name(), store.getTimezone(),
                value.getStreet(), value.getStreetNumber(), value.getAddressComplement(), value.getNeighborhood(),
                value.getCity(), value.getStateCode(), value.getPostalCode(), value.getLatitude(), value.getLongitude(),
                value.acceptsPix(), value.acceptsCard(), value.acceptsCashOnDelivery(), value.getDeliveryFee(),
                value.getPreparationMinutes(), value.getVersion());
    }

    private DeliveryZoneResponse response(DeliveryZone zone) {
        return new DeliveryZoneResponse(zone.getCenterLatitude(), zone.getCenterLongitude(), zone.getRadiusMeters(),
                zone.isActive(), zone.getPolicyVersion(), zone.getVersion());
    }

    private ZoneId parseTimezone(String value) {
        try {
            return ZoneId.of(value);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Fuso horário inválido.");
        }
    }

    private void record(String action, Store store) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID actorId)) {
            throw new AccessDeniedException("An authenticated store owner is required.");
        }
        audit.record(action, "store", store.getId(), actorId, organizationId());
    }
}
