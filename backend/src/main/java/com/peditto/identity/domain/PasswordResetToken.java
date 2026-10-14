package com.peditto.identity.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.identity.domain.AppUser;

@Entity
@Table(name="password_reset_tokens")
public class PasswordResetToken extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="user_id",nullable=false)
    private AppUser user;
    @Column(name="token_hash",nullable=false,length=64)
    private String tokenHash;
    @Column(name="expires_at",nullable=false)
    private Instant expiresAt;
    @Column(name="consumed_at")
    private Instant consumedAt;
    @Column(name="invalidated_at")
    private Instant invalidatedAt;
    protected PasswordResetToken() {}
}

