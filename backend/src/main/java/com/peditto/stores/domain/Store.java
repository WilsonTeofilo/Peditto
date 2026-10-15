package com.peditto.stores.domain;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "stores",
        indexes = @Index(name = "ix_stores_organization_id", columnList = "organization_id"),
        uniqueConstraints = {
        @UniqueConstraint(name = "uk_stores_organization_id", columnNames = {"organization_id", "id"}),
        @UniqueConstraint(name = "uk_stores_one_per_organization", columnNames = {"organization_id"})
})
public class Store extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @Column(nullable = false, unique = true, length = StorePolicy.MAX_STORE_SLUG_LENGTH)
    private String slug;

    @Column(name = "display_name", nullable = false, length = StorePolicy.MAX_STORE_NAME_LENGTH)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StoreStatus status;

    @Column(nullable = false, length = StorePolicy.MAX_TIMEZONE_LENGTH)
    private String timezone;

    @Version
    @Column(nullable = false)
    private long version;

    protected Store() {}

    public Store(Organization organization, String slug, String displayName, ZoneId timezone) {
        this.organization = organization;
        this.slug = slug;
        this.displayName = displayName;
        this.timezone = timezone.getId();
        this.status = StoreStatus.SETUP_REQUIRED;
    }

    public Organization getOrganization() { return organization; }
    public String getSlug() { return slug; }
    public String getDisplayName() { return displayName; }
    public StoreStatus getStatus() { return status; }
    public String getTimezone() { return timezone; }

    public void updateProfile(String displayName, ZoneId timezone) {
        if (displayName == null || displayName.isBlank()
                || displayName.strip().length() > StorePolicy.MAX_STORE_NAME_LENGTH || timezone == null) {
            throw new IllegalArgumentException("Perfil da loja inválido.");
        }
        this.displayName = displayName.strip();
        this.timezone = timezone.getId();
    }

    public void publish() {
        if (status != StoreStatus.SETUP_REQUIRED && status != StoreStatus.DRAFT) {
            throw new IllegalStateException("A loja não pode ser publicada a partir do estado atual.");
        }
        status = StoreStatus.ACTIVE;
    }

    public void closeForOrders() {
        if (status == StoreStatus.ACTIVE) {
            status = StoreStatus.CLOSED;
            return;
        }
        if (status != StoreStatus.CLOSED) {
            throw new IllegalStateException("A loja não está publicada.");
        }
    }

    public void reopenForOrders() {
        if (status != StoreStatus.CLOSED) {
            throw new IllegalStateException("A loja não está fechada pelo responsável.");
        }
        status = StoreStatus.ACTIVE;
    }
}

