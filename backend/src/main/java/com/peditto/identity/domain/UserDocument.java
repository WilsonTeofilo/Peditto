package com.peditto.identity.domain;

import com.peditto.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_documents", indexes = @Index(name = "ix_user_documents_user_id", columnList = "user_id"))
public class UserDocument extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 4)
    private DocumentType documentType;

    @Column(name = "document_normalized", nullable = false, unique = true, length = 14)
    private String documentNormalized;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    protected UserDocument() {}

    public UserDocument(AppUser user, DocumentType type, String document) {
        this.user = user;
        this.documentType = type;
        this.documentNormalized = document;
    }

    public AppUser getUser() { return user; }
    public DocumentType getDocumentType() { return documentType; }
    public String getDocumentNormalized() { return documentNormalized; }
    public Instant getVerifiedAt() { return verifiedAt; }

    public void verify(Instant verifiedAt) {
        if (this.verifiedAt == null) this.verifiedAt = verifiedAt;
    }
}
