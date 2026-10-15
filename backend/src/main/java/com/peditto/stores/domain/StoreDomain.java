package com.peditto.stores.domain;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "store_domains", indexes = @Index(name = "ix_store_domains_store_id", columnList = "store_id"))
public class StoreDomain extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    private Store store;

    @Column(name = "hostname_normalized", nullable = false, unique = true, length = 253)
    private String hostnameNormalized;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(nullable = false)
    private boolean active;

    @Version
    @Column(nullable = false)
    private long version;

    protected StoreDomain() {}

    public StoreDomain(Organization organization, Store store, String normalizedHost) {
        this.organization = organization;
        this.store = store;
        this.hostnameNormalized = normalizedHost;
    }

    public Store getStore() { return store; }
    public String getHostnameNormalized() { return hostnameNormalized; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public boolean isActive() { return active; }
}
