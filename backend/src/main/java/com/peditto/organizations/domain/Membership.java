package com.peditto.organizations.domain;

import com.peditto.common.persistence.BaseEntity;
import com.peditto.identity.domain.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "memberships", indexes = {
        @Index(name = "ix_memberships_org_status", columnList = "organization_id,status"),
        @Index(name = "ix_memberships_user_status", columnList = "user_id,status")
}, uniqueConstraints = @UniqueConstraint(name = "uk_memberships_organization_user",
        columnNames = {"organization_id", "user_id"}))
public class Membership extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status;

    protected Membership() {}

    public Membership(Organization organization, AppUser user, MembershipRole role) {
        this.organization = organization;
        this.user = user;
        this.role = role;
        this.status = MembershipStatus.INVITED;
    }

    public Organization getOrganization() { return organization; }
    public AppUser getUser() { return user; }
    public MembershipRole getRole() { return role; }
    public MembershipStatus getStatus() { return status; }

    public void activate() {
        if (status != MembershipStatus.INVITED) {
            throw new IllegalStateException("Membership cannot be activated.");
        }
        status = MembershipStatus.ACTIVE;
    }
}
