package com.peditto.cart.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import com.peditto.stores.domain.Store;
import com.peditto.cart.domain.ShoppingCart;
import com.peditto.catalog.domain.Product;

@Entity
@Table(name="cart_items")
public class CartItem extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="organization_id",nullable=false)
    private Organization organization;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="store_id",nullable=false)
    private Store store;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="cart_id",nullable=false)
    private ShoppingCart cart;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="product_id",nullable=false)
    private Product product;
    @Column(nullable=false)
    private short quantity;
    @Column(name="configuration_hash",nullable=false,length=64)
    private String configurationHash;
    @Column(name="customer_note",length=300)
    private String customerNote;
    @Version
    @Column(nullable=false)
    private long version;
    protected CartItem() {}
}

