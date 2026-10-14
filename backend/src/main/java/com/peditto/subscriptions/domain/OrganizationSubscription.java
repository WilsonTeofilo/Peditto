package com.peditto.subscriptions.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import com.peditto.subscriptions.domain.PlanPriceVersion;

@Entity
@Table(name="organization_subscriptions")
public class OrganizationSubscription extends BaseEntity {
    @OneToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="organization_id",nullable=false,unique=true)
    private Organization organization;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="plan_price_version_id",nullable=false)
    private PlanPriceVersion planPriceVersion;
    @Column(nullable=false,length=24)
    private String status;
    @Column(name="current_period_start",nullable=false)
    private Instant currentPeriodStart;
    @Column(name="current_period_end",nullable=false)
    private Instant currentPeriodEnd;
    @Column(name="trial_ends_at")
    private Instant trialEndsAt;
    @Column(name="grace_ends_at")
    private Instant graceEndsAt;
    @Version
    @Column(nullable=false)
    private long version;
    protected OrganizationSubscription() {}
}

