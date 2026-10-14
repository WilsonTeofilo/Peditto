package com.peditto.cart.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.catalog.domain.ProductAddon;

@Entity
@Table(name="cart_item_addons")
public class CartItemAddon extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="cart_item_id",nullable=false)
    private CartItem cartItem;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="addon_id",nullable=false)
    private ProductAddon addon;
    @Column(nullable=false)
    private short quantity;
    protected CartItemAddon() {}
}

