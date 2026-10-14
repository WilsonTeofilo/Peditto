package com.peditto.subscriptions.domain;

import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "billing_invoices",
indexes = @Index(name = "ix_billing_invoices_org_due", columnList = "organization_id,due_at,status"))
public class BillingInvoice extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "provider_reference", length = 160)
    private String providerReference;

    @Version
    @Column(nullable = false)
    private long version;

    protected BillingInvoice() {}

    public BillingInvoice(Organization organization, Instant periodStart, Instant periodEnd,
    Instant dueAt, BigDecimal amount) {
        if (periodStart == null || periodEnd == null || !periodEnd.isAfter(periodStart)
        || amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Período ou valor de fatura inválido.");
        }
        this.organization = organization;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.dueAt = dueAt;
        this.amount = amount;
        this.status = InvoiceStatus.OPEN;
    }

    public Organization getOrganization() { return organization; }
    public Instant getDueAt() { return dueAt; }
    public BigDecimal getAmount() { return amount; }
    public InvoiceStatus getStatus() { return status; }
}

