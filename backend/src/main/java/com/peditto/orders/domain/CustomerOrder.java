package com.peditto.orders.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import com.peditto.stores.domain.Store;
import com.peditto.customers.domain.CustomerProfile;

@Entity
@Table(name="customer_orders")
public class CustomerOrder extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="organization_id",nullable=false)
    private Organization organization;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="store_id",nullable=false)
    private Store store;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="customer_id")
    private CustomerProfile customer;
    @Column(name="order_number",nullable=false,length=20)
    private String orderNumber;
    @Column(nullable=false,length=24)
    private String status;
    @Column(name="payment_status",nullable=false,length=20)
    private String paymentStatus;
    @Column(name="fulfillment_type",nullable=false,length=16)
    private String fulfillmentType;
    @Column(nullable=false,precision=12,scale=2)
    private BigDecimal subtotal;
    @Column(name="delivery_fee",nullable=false,precision=12,scale=2)
    private BigDecimal deliveryFee;
    @Column(name="discount_amount",nullable=false,precision=12,scale=2)
    private BigDecimal discountAmount;
    @Column(name="total_amount",nullable=false,precision=12,scale=2)
    private BigDecimal totalAmount;
    @Column(name="customer_name_snapshot",nullable=false,length=120)
    private String customerNameSnapshot;
    @Column(name="customer_phone_snapshot",nullable=false,length=16)
    private String customerPhoneSnapshot;
    @Version
    @Column(nullable=false)
    private long version;
    protected CustomerOrder() {}
}

