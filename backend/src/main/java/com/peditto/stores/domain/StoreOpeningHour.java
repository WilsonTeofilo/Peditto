package com.peditto.stores.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import com.peditto.stores.domain.Store;

@Entity
@Table(name="store_opening_hours")
public class StoreOpeningHour extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="organization_id",nullable=false)
    private Organization organization;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="store_id",nullable=false)
    private Store store;
    @Column(nullable=false)
    private short weekday;
    @Column(nullable=false)
    private short slot;
    @Column(name="opens_at")
    private LocalTime opensAt;
    @Column(name="closes_at")
    private LocalTime closesAt;
    @Column(nullable=false)
    private boolean closed;
    @Version
    @Column(nullable=false)
    private long version;
    protected StoreOpeningHour() {}
}

