package com.peditto.cart.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import com.peditto.stores.domain.Store;
import com.peditto.customers.domain.CustomerProfile;

@Entity
@Table(name="shopping_carts")
public class ShoppingCart extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="organization_id",nullable=false)
    private Organization organization;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="store_id",nullable=false)
    private Store store;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="customer_id")
    private CustomerProfile customer;
    @Column(name="anonymous_token_hash",length=64)
    private String anonymousTokenHash;
    @Column(nullable=false,length=16)
    private String status;
    @Column(name="expires_at",nullable=false)
    private Instant expiresAt;
    @Version
    @Column(nullable=false)
    private long version;
    protected ShoppingCart() {}
}

