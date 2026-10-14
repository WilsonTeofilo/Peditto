package com.peditto.orders.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import com.peditto.stores.domain.Store;
import com.peditto.orders.domain.CustomerOrder;
import com.peditto.catalog.domain.Product;

@Entity
@Table(name="order_items")
public class OrderItem extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="organization_id",nullable=false)
    private Organization organization;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="store_id",nullable=false)
    private Store store;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="order_id",nullable=false)
    private CustomerOrder order;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="product_id")
    private Product product;
    @Column(name="product_name_snapshot",nullable=false,length=140)
    private String productNameSnapshot;
    @Column(nullable=false)
    private short quantity;
    @Column(name="unit_price_snapshot",nullable=false,precision=12,scale=2)
    private BigDecimal unitPriceSnapshot;
    @Column(name="unit_cost_snapshot",precision=12,scale=2)
    private BigDecimal unitCostSnapshot;
    @Column(name="line_total",nullable=false,precision=12,scale=2)
    private BigDecimal lineTotal;
    @Version
    @Column(nullable=false)
    private long version;
    protected OrderItem() {}
}

