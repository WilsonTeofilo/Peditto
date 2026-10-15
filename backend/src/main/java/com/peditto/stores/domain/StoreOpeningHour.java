package com.peditto.stores.domain;

import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalTime;

@Entity
@Table(name = "store_opening_hours")
public class StoreOpeningHour extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @Column(nullable = false)
    private short weekday;

    @Column(nullable = false)
    private short slot;

    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    @Column(nullable = false)
    private boolean closed;

    @Version
    @Column(nullable = false)
    private long version;

    protected StoreOpeningHour() {}

    public StoreOpeningHour(Organization organization, Store store, short weekday, short slot,
            LocalTime opensAt, LocalTime closesAt) {
        if (organization == null || store == null
                || !organization.getId().equals(store.getOrganization().getId())
                || weekday < 0 || weekday >= StorePolicy.DAYS_PER_WEEK
                || slot < 0 || slot >= StorePolicy.MAX_INTERVALS_PER_DAY
                || opensAt == null || closesAt == null
                || !closesAt.isAfter(opensAt)) {
            throw new IllegalArgumentException("Intervalo de funcionamento inválido.");
        }
        this.organization = organization;
        this.store = store;
        this.weekday = weekday;
        this.slot = slot;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
    }

    public short getWeekday() { return weekday; }
    public short getSlot() { return slot; }
    public LocalTime getOpensAt() { return opensAt; }
    public LocalTime getClosesAt() { return closesAt; }
    public boolean isClosed() { return closed; }
}

