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

@Entity
@Table(name = "login_pre_authentications", indexes = @Index(name = "ix_login_pre_auth_user_expiry", columnList = "user_id,expires_at"))
public class LoginPreAuthentication extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private AppUser user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "consumed_at")
    private Instant consumedAt;
    @Column(nullable = false)
    private short attempts;
    @Version
    @Column(nullable = false)
    private long version;
    protected LoginPreAuthentication() {}

    public LoginPreAuthentication(AppUser user, String tokenHash, Instant expiresAt) {
        this.user = user; this.tokenHash = tokenHash; this.expiresAt = expiresAt;
    }

    public AppUser getUser() { return user; }

    public boolean isUsableAt(Instant now) { return consumedAt == null && expiresAt.isAfter(now) && attempts < 10; }

    public short getAttempts() { return attempts; }

    public void recordFailedAttempt() { if (attempts < 10) attempts++; }

    public void consume(Instant now) { if (!isUsableAt(now)) throw new IllegalStateException("Pre-authentication unavailable."); consumedAt = now; }
}

