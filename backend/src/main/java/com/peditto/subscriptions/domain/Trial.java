package com.peditto.subscriptions.domain;

import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "trials")
public class Trial extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, unique = true, updatable = false)
    private Organization organization;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrialStatus status;

    @Column(name = "rule_version", nullable = false, length = 32)
    private String ruleVersion;

    protected Trial() {}

    public Trial(Organization organization, Instant startsAt, Instant endsAt, String ruleVersion) {
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Fim do trial deve ser após o início.");
        }
        this.organization = organization;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = TrialStatus.SCHEDULED;
        this.ruleVersion = ruleVersion;
    }

    public Organization getOrganization() { return organization; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public TrialStatus getStatus() { return status; }

    public void activate() {
        if (status != TrialStatus.SCHEDULED) {
            throw new IllegalStateException("Trial cannot be activated.");
        }
        status = TrialStatus.ACTIVE;
    }
}
