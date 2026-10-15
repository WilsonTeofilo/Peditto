package com.peditto.catalog.domain;

import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import com.peditto.stores.domain.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_addons")
public class ProductAddon extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(nullable = false, length = CatalogPolicy.MAX_ADDON_NAME_LENGTH)
    private String name;

    @Column(length = CatalogPolicy.MAX_ADDON_DESCRIPTION_LENGTH)
    private String description;

    @Column(name = "price_delta", nullable = false,
            precision = CatalogPolicy.MONEY_PRECISION, scale = CatalogPolicy.MONEY_SCALE)
    private BigDecimal priceDelta;

    @Column(name = "max_quantity", nullable = false)
    private short maxQuantity;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected ProductAddon() {}

    public ProductAddon(Organization organization, Store store, Product product, String name,
            String description, BigDecimal priceDelta, short maxQuantity) {
        if (organization == null || store == null || product == null
                || !organization.getId().equals(store.getOrganization().getId())
                || !store.getId().equals(product.getStoreId())
                || !organization.getId().equals(product.getOrganizationId())) {
            throw new IllegalArgumentException("Produto pertence a outra loja.");
        }
        this.organization = organization;
        this.store = store;
        this.product = product;
        update(name, description, priceDelta, maxQuantity);
    }

    public void update(String name, String description, BigDecimal priceDelta, short maxQuantity) {
        validateName(name);
        if (description != null && description.length() > CatalogPolicy.MAX_ADDON_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Descrição do adicional excede o limite.");
        }
        if (maxQuantity < 1 || maxQuantity > CatalogPolicy.MAX_ADDON_QUANTITY) {
            throw new IllegalArgumentException("Quantidade máxima do adicional inválida.");
        }
        this.name = name.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.priceDelta = normalizePrice(priceDelta);
        this.maxQuantity = maxQuantity;
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

    public UUID getId() { return super.getId(); }
    public UUID getStoreId() { return store.getId(); }
    public UUID getProductId() { return product.getId(); }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPriceDelta() { return priceDelta; }
    public short getMaxQuantity() { return maxQuantity; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.strip().length() > CatalogPolicy.MAX_ADDON_NAME_LENGTH) {
            throw new IllegalArgumentException("Nome do adicional inválido.");
        }
    }

    private static BigDecimal normalizePrice(BigDecimal price) {
        if (price == null || price.signum() < 0) throw new IllegalArgumentException("Preço do adicional inválido.");
        try {
            BigDecimal normalized = price.setScale(CatalogPolicy.MONEY_SCALE, RoundingMode.UNNECESSARY);
            if (normalized.precision() > CatalogPolicy.MONEY_PRECISION) {
                throw new IllegalArgumentException("Preço do adicional excede o limite.");
            }
            return normalized;
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Preço deve ter no máximo duas casas decimais.");
        }
    }
}
