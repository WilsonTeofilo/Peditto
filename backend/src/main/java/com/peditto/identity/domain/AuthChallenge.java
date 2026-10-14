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
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "auth_challenges", indexes = @Index(name = "ix_auth_challenges_user_purpose", columnList = "user_id,purpose,expires_at"))
public class AuthChallenge extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private AppUser user;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AuthChallengePurpose purpose;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private OtpChannel channel;
    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private short attempts;
    @Column(name = "consumed_at")
    private Instant consumedAt;
    @Version
    @Column(nullable = false)
    private long version;
    protected AuthChallenge() {}

    public AuthChallenge(AppUser user, AuthChallengePurpose purpose, OtpChannel channel, String codeHash, Instant expiresAt) {
        this.user = user; this.purpose = purpose; this.channel = channel; this.codeHash = codeHash;
        this.expiresAt = expiresAt; this.attempts = 0;
    }

    public AppUser getUser() { return user; }

    public AuthChallengePurpose getPurpose() { return purpose; }

    public OtpChannel getChannel() { return channel; }

    public String getCodeHash() { return codeHash; }

    public Instant getExpiresAt() { return expiresAt; }

    public short getAttempts() { return attempts; }

    public Instant getConsumedAt() { return consumedAt; }

    public boolean isUsableAt(Instant now) { return consumedAt == null && expiresAt.isAfter(now) && attempts < 10; }

    public void recordFailedAttempt() { if (attempts < 10) attempts++; }

    public void consume(Instant now) { if (!isUsableAt(now)) throw new IllegalStateException("Desafio indisponível."); consumedAt = now; }
}

