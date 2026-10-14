package com.peditto.orders.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import com.peditto.stores.domain.Store;
import com.peditto.orders.domain.CustomerOrder;

@Entity
@Table(name="order_payments")
public class OrderPayment extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="organization_id",nullable=false)
    private Organization organization;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="store_id",nullable=false)
    private Store store;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="order_id",nullable=false)
    private CustomerOrder order;
    @Column(nullable=false,length=24)
    private String method;
    @Column(nullable=false,length=20)
    private String status;
    @Column(nullable=false,precision=12,scale=2)
    private BigDecimal amount;
    @Column(name="provider",length=40)
    private String provider;
    @Column(name="provider_payment_reference",length=180)
    private String providerPaymentReference;
    @Column(name="provider_idempotency_key",length=160)
    private String providerIdempotencyKey;
    @Version
    @Column(nullable=false)
    private long version;
    protected OrderPayment() {}
}

