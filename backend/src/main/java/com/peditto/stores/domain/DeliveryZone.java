package com.peditto.stores.domain;

import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "delivery_zones",
indexes = @Index(name = "ix_delivery_zones_tenant_active", columnList = "organization_id,store_id,active"),
uniqueConstraints = @UniqueConstraint(name = "uk_delivery_zones_store_id", columnNames = "store_id"))
public class DeliveryZone extends BaseEntity {
    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);
    private static final int MIN_RADIUS_METERS = 100;
    private static final int MAX_RADIUS_METERS = 100_000;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false, unique = true)
    private Store store;

    @Column(name = "center_latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal centerLatitude;
    @Column(name = "center_longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal centerLongitude;
    @Column(name = "radius_meters", nullable = false)
    private int radiusMeters;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "policy_version", nullable = false)
    private int policyVersion = 1;
    @Version
    @Column(nullable = false)
    private long version;
    protected DeliveryZone() {}

    public DeliveryZone(Organization organization, Store store, BigDecimal latitude, BigDecimal longitude, int radiusMeters) {
        if (!isValidCenter(latitude, longitude) || !isValidRadius(radiusMeters)) {
            throw new IllegalArgumentException("Coordenadas ou raio fora dos limites permitidos.");
        }
        this.organization = organization;
        this.store = store;
        this.centerLatitude = latitude;
        this.centerLongitude = longitude;
        this.radiusMeters = radiusMeters;
    }

    private static boolean isValidCenter(BigDecimal latitude, BigDecimal longitude) {
        return latitude != null && longitude != null
        && latitude.compareTo(MIN_LATITUDE) >= 0 && latitude.compareTo(MAX_LATITUDE) <= 0
        && longitude.compareTo(MIN_LONGITUDE) >= 0 && longitude.compareTo(MAX_LONGITUDE) <= 0;
    }

    private static boolean isValidRadius(int radiusMeters) {
        return radiusMeters >= MIN_RADIUS_METERS && radiusMeters <= MAX_RADIUS_METERS;
    }

    public UUID getStoreId() { return store.getId(); }

    public int getRadiusMeters() { return radiusMeters; }

    public boolean isActive() { return active; }

    public long getVersion() { return version; }
}

