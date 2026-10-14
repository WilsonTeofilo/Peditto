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
@Table(name = "store_settings",
indexes = @Index(name = "ix_store_settings_tenant", columnList = "organization_id,store_id"),
uniqueConstraints = @UniqueConstraint(name = "uk_store_settings_store_id", columnNames = "store_id"))
public class StoreSettings extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false, unique = true)
    private Store store;

    @Column(length = 180)
    private String street;
    @Column(name = "street_number", length = 30)
    private String streetNumber;
    @Column(name = "address_complement", length = 120)
    private String addressComplement;
    @Column(length = 120)
    private String neighborhood;
    @Column(length = 120)
    private String city;
    @Column(name = "state_code", length = 2)
    private String stateCode;
    @Column(name = "postal_code", length = 9)
    private String postalCode;
    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;
    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;
    @Column(name = "accepts_pix", nullable = false)
    private boolean acceptsPix = true;
    @Column(name = "accepts_card", nullable = false)
    private boolean acceptsCard;
    @Column(name = "accepts_cash_on_delivery", nullable = false)
    private boolean acceptsCashOnDelivery;
    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;
    @Column(name = "preparation_minutes", nullable = false)
    private int preparationMinutes = 30;
    @Version
    @Column(nullable = false)
    private long version;
    protected StoreSettings() {}

    public StoreSettings(Organization organization, Store store) {
        this.organization = organization;
        this.store = store;
    }

    public UUID getStoreId() { return store.getId(); }

    public BigDecimal getDeliveryFee() { return deliveryFee; }

    public long getVersion() { return version; }
}

