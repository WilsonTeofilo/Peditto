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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_price_history")
public class ProductPriceHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID actorUserId;

    @Column(name = "old_price", precision = CatalogPolicy.MONEY_PRECISION,
            scale = CatalogPolicy.MONEY_SCALE, updatable = false)
    private BigDecimal oldPrice;

    @Column(name = "new_price", nullable = false, precision = CatalogPolicy.MONEY_PRECISION,
            scale = CatalogPolicy.MONEY_SCALE, updatable = false)
    private BigDecimal newPrice;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected ProductPriceHistory() {}

    public ProductPriceHistory(Organization organization, Store store, Product product,
            BigDecimal oldPrice, BigDecimal newPrice, UUID actorUserId, Instant changedAt) {
        this.organization = organization;
        this.store = store;
        this.product = product;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.actorUserId = actorUserId;
        this.changedAt = changedAt;
    }
}
