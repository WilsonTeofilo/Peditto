package com.peditto.organizations.domain;

import com.peditto.common.persistence.BaseEntity;
import com.peditto.identity.domain.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "organizations", indexes = @Index(name = "ix_organizations_status", columnList = "status"))
public class Organization extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 4)
    private DocumentType documentType;

    @Column(name = "document_normalized", nullable = false, unique = true, length = 14)
    private String documentNormalized;

    @Column(name = "legal_name", length = 160)
    private String legalName;

    @Column(name = "trade_name", nullable = false, length = 120)
    private String tradeName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrganizationStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    protected Organization() {}

    public Organization(DocumentType type, String document, String legalName, String tradeName) {
        this.documentType = type;
        this.documentNormalized = document;
        this.legalName = legalName;
        this.tradeName = tradeName;
        this.status = OrganizationStatus.PENDING_VERIFICATION;
    }

    public DocumentType getDocumentType() { return documentType; }
    public String getDocumentNormalized() { return documentNormalized; }
    public String getLegalName() { return legalName; }
    public String getTradeName() { return tradeName; }
    public OrganizationStatus getStatus() { return status; }

    public void activate() {
        if (status != OrganizationStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Organization cannot be activated.");
        }
        status = OrganizationStatus.ACTIVE;
    }
}

