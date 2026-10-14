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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalog_categories", indexes = @Index(name = "ix_categories_tenant_active_order", columnList = "organization_id,store_id,active,display_order"))
public class CatalogCategory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "archived_at")
    private Instant archivedAt;
    @Version
    @Column(nullable = false)
    private long version;
    protected CatalogCategory() {}

    public CatalogCategory(Organization organization, Store store, String name, int displayOrder) {
        this.organization = organization;
        this.store = store;
        this.name = name;
        this.displayOrder = displayOrder;
    }

    public UUID getStoreId() { return store.getId(); }

    public String getName() { return name; }

    public boolean isActive() { return active; }
}

