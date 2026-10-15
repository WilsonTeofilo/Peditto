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
@Table(name = "catalog_categories", indexes = @Index(name = "ix_categories_tenant_active_order",
        columnList = "organization_id,store_id,active,display_order"))
public class CatalogCategory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @Column(nullable = false, length = CatalogPolicy.MAX_CATEGORY_NAME_LENGTH)
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
        if (organization == null || store == null
                || !organization.getId().equals(store.getOrganization().getId())) {
            throw new IllegalArgumentException("Loja não pertence à organização informada.");
        }
        validateName(name);
        if (displayOrder < 0) throw new IllegalArgumentException("Ordem da categoria inválida.");
        this.organization = organization;
        this.store = store;
        this.name = name.strip();
        this.displayOrder = displayOrder;
    }

    public void rename(String name) {
        validateName(name);
        this.name = name.strip();
    }

    public void moveTo(int displayOrder) {
        if (displayOrder < 0) throw new IllegalArgumentException("Ordem da categoria inválida.");
        this.displayOrder = displayOrder;
    }

    public void archive(Instant archivedAt) {
        if (archivedAt == null) throw new IllegalArgumentException("Data de arquivamento obrigatória.");
        this.active = false;
        this.archivedAt = archivedAt;
    }

    public void restore() {
        this.active = true;
        this.archivedAt = null;
    }

    public UUID getId() { return super.getId(); }
    public UUID getStoreId() { return store.getId(); }
    public UUID getOrganizationId() { return organization.getId(); }
    public String getName() { return name; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.strip().length() > CatalogPolicy.MAX_CATEGORY_NAME_LENGTH) {
            throw new IllegalArgumentException("Nome da categoria inválido.");
        }
    }
}

