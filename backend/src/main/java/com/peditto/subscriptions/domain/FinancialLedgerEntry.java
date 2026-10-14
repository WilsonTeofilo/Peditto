package com.peditto.subscriptions.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;

@Entity
@Table(name="financial_ledger_entries")
public class FinancialLedgerEntry extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="organization_id")
    private Organization organization;
    @Column(name="source_type",nullable=false,length=24)
    private String sourceType;
    @Column(name="source_id",nullable=false)
    private UUID sourceId;
    @Column(name="entry_type",nullable=false,length=40)
    private String entryType;
    @Column(nullable=false,precision=14,scale=2)
    private BigDecimal amount;
    @Column(nullable=false,length=3)
    private String currency;
    @Column(name="occurred_at",nullable=false)
    private Instant occurredAt;
    @Column(name="idempotency_key",nullable=false,length=180)
    private String idempotencyKey;
    @Version
    @Column(nullable=false)
    private long version;
    protected FinancialLedgerEntry() {}
}

