package com.peditto.catalog.domain;

import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import com.peditto.stores.domain.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "ix_products_tenant_active", columnList = "organization_id,store_id,active,name"),
    @Index(name = "ix_products_tenant_category", columnList = "organization_id,store_id,category_id")
    })
public class Product extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CatalogCategory category;

    @Column(nullable = false, length = 140)
    private String name;
    @Column(length = 2000)
    private String description;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "archived_at")
    private Instant archivedAt;
    @Version
    @Column(nullable = false)
    private long version;
    protected Product() {}

    public Product(Organization organization, Store store, CatalogCategory category, String name, String description, BigDecimal price) {
        if (price == null || price.signum() < 0) throw new IllegalArgumentException("Preço inválido.");
        if (category != null && !store.getId().equals(category.getStoreId())) throw new IllegalArgumentException("Categoria pertence a outra loja.");
        this.organization = organization;
        this.store = store;
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public UUID getStoreId() { return store.getId(); }

    public BigDecimal getPrice() { return price; }

    public boolean isActive() { return active; }
}

