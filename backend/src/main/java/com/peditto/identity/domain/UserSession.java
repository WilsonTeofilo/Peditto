package com.peditto.identity.domain;

import com.peditto.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_sessions", indexes = @Index(name = "ix_user_sessions_user_live", columnList = "user_id,expires_at"))
public class UserSession extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private AppUser user;
    @Column(name = "family_id", nullable = false)
    private UUID familyId;
    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
    private String refreshTokenHash;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "last_used_at")
    private Instant lastUsedAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Column(name = "revoke_reason", length = 40)
    private String revokeReason;
    @Column(name = "replaced_by_session_id")
    private UUID replacedBySessionId;
    @Version
    @Column(nullable = false)
    private long version;
    protected UserSession() {}

    public UserSession(AppUser user, UUID familyId, String tokenHash, Instant expiresAt) {
        this.user = user; this.familyId = familyId; this.refreshTokenHash = tokenHash; this.expiresAt = expiresAt;
        this.lastUsedAt = Instant.now();
    }

    public AppUser getUser() { return user; }

    public UUID getFamilyId() { return familyId; }

    public Instant getExpiresAt() { return expiresAt; }

    public Instant getLastUsedAt() { return lastUsedAt; }

    public boolean matchesTokenHash(String tokenHash) { return refreshTokenHash.equals(tokenHash); }

    public Instant getRevokedAt() { return revokedAt; }

    public boolean isExpiredAt(Instant now) { return !expiresAt.isAfter(now); }

    public boolean isUsableAt(Instant now) { return revokedAt == null && expiresAt.isAfter(now) && user.isEnabled(); }

    public void markReplacedBy(UUID replacementId) {
        if (replacementId == null || replacementId.equals(getId())) throw new IllegalArgumentException("Invalid replacement session.");
        replacedBySessionId = replacementId;
    }

    public void touch(Instant now) { lastUsedAt = now; }

    public void revoke(Instant now, String reason) {
        if (reason == null || !reason.matches("[A-Z_]{2,40}")) throw new IllegalArgumentException("Invalid revoke reason.");
        if (revokedAt == null) { revokedAt = now; revokeReason = reason; }
    }
}

