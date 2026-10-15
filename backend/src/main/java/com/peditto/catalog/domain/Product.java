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
import java.math.RoundingMode;
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

    @Column(nullable = false, length = CatalogPolicy.MAX_PRODUCT_NAME_LENGTH)
    private String name;
    @Column(length = CatalogPolicy.MAX_PRODUCT_DESCRIPTION_LENGTH)
    private String description;
    @Column(nullable = false, precision = CatalogPolicy.MONEY_PRECISION, scale = CatalogPolicy.MONEY_SCALE)
    private BigDecimal price;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "available_for_sale", nullable = false)
    private boolean availableForSale = true;
    @Column(name = "archived_at")
    private Instant archivedAt;
    @Version
    @Column(nullable = false)
    private long version;
    protected Product() {}

    public Product(Organization organization, Store store, CatalogCategory category, String name, String description, BigDecimal price) {
        if (organization == null || store == null
                || !organization.getId().equals(store.getOrganization().getId())) {
            throw new IllegalArgumentException("Loja não pertence à organização informada.");
        }
        this.organization = organization;
        this.store = store;
        update(category, name, description, price);
    }

    public void update(CatalogCategory category, String name, String description, BigDecimal price) {
        validateName(name);
        if (description != null && description.length() > CatalogPolicy.MAX_PRODUCT_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Descrição do produto excede o limite.");
        }
        if (category != null && (!store.getId().equals(category.getStoreId())
                || !organization.getId().equals(category.getOrganizationId()))) {
            throw new IllegalArgumentException("Categoria pertence a outra loja.");
        }
        this.category = category;
        this.name = name.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.price = normalizePrice(price);
    }

    public void archive(Instant archivedAt) {
        if (archivedAt == null) throw new IllegalArgumentException("Data de arquivamento obrigatória.");
        active = false;
        this.archivedAt = archivedAt;
    }

    public void restore() {
        active = true;
        archivedAt = null;
    }

    public void setAvailableForSale(boolean availableForSale) {
        if (!active && availableForSale) {
            throw new IllegalStateException("Produto arquivado não pode ficar disponível para venda.");
        }
        this.availableForSale = availableForSale;
    }

    public UUID getId() { return super.getId(); }
    public UUID getStoreId() { return store.getId(); }
    public UUID getOrganizationId() { return organization.getId(); }
    public UUID getCategoryId() { return category == null ? null : category.getId(); }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public boolean isActive() { return active; }
    public boolean isAvailableForSale() { return availableForSale; }
    public long getVersion() { return version; }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.strip().length() > CatalogPolicy.MAX_PRODUCT_NAME_LENGTH) {
            throw new IllegalArgumentException("Nome do produto inválido.");
        }
    }

    private static BigDecimal normalizePrice(BigDecimal price) {
        if (price == null || price.signum() < 0) throw new IllegalArgumentException("Preço inválido.");
        try {
            BigDecimal normalized = price.setScale(CatalogPolicy.MONEY_SCALE, RoundingMode.UNNECESSARY);
            if (normalized.precision() > CatalogPolicy.MONEY_PRECISION) {
                throw new IllegalArgumentException("Preço excede o limite.");
            }
            return normalized;
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Preço deve ter no máximo duas casas decimais.");
        }
    }
}

