package com.peditto.customers.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import com.peditto.stores.domain.Store;

@Entity
@Table(name="customer_profiles")
public class CustomerProfile extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="organization_id",nullable=false)
    private Organization organization;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="store_id",nullable=false)
    private Store store;
    @Column(name="display_name",nullable=false,length=120)
    private String displayName;
    @Column(name="email_normalized",length=254)
    private String emailNormalized;
    @Column(name="phone_e164",length=16)
    private String phoneE164;
    @Column(name="contact_verified_at")
    private Instant contactVerifiedAt;
    @Column(name="marketing_consent_at")
    private Instant marketingConsentAt;
    @Column(name="marketing_consent_version",length=40)
    private String marketingConsentVersion;
    @Column(nullable=false,length=16)
    private String status;
    @Version
    @Column(nullable=false)
    private long version;
    protected CustomerProfile() {}
}

