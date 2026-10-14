package com.peditto.subscriptions.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import com.peditto.subscriptions.domain.BillingInvoice;

@Entity
@Table(name="invoice_payments")
public class InvoicePayment extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="organization_id",nullable=false)
    private Organization organization;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="invoice_id",nullable=false)
    private BillingInvoice invoice;
    @Column(nullable=false,length=12)
    private String method;
    @Column(nullable=false,length=20)
    private String status;
    @Column(nullable=false,precision=12,scale=2)
    private BigDecimal amount;
    @Column(nullable=false,length=40)
    private String provider;
    @Column(name="provider_payment_reference",length=180)
    private String providerPaymentReference;
    @Column(name="provider_idempotency_key",nullable=false,length=160)
    private String providerIdempotencyKey;
    @Version
    @Column(nullable=false)
    private long version;
    protected InvoicePayment() {}
}

