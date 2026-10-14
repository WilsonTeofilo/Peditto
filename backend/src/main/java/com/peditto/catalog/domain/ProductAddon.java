package com.peditto.catalog.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import com.peditto.stores.domain.Store;
import com.peditto.catalog.domain.Product;

@Entity
@Table(name="product_addons")
public class ProductAddon extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="organization_id",nullable=false)
    private Organization organization;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="store_id",nullable=false)
    private Store store;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="product_id",nullable=false)
    private Product product;
    @Column(nullable=false,length=100)
    private String name;
    @Column(length=500)
    private String description;
    @Column(name="price_delta",nullable=false,precision=12,scale=2)
    private BigDecimal priceDelta;
    @Column(name="max_quantity",nullable=false)
    private short maxQuantity;
    @Column(nullable=false)
    private boolean active;
    @Column(name="archived_at")
    private Instant archivedAt;
    @Version
    @Column(nullable=false)
    private long version;
    protected ProductAddon() {}
}

