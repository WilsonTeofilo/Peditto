package com.peditto.identity.domain;

import com.peditto.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "app_users", indexes = @Index(name = "ix_app_users_enabled", columnList = "enabled"))
public class AppUser extends BaseEntity {
    @Column(name = "email_normalized", nullable = false, unique = true, length = 254)
    private String emailNormalized;

    @Column(name = "phone_e164", nullable = false, unique = true, length = 16)
    private String phoneE164;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    @Column(nullable = false)
    private boolean enabled;

    @Version
    @Column(nullable = false)
    private long version;

    protected AppUser() {}

    public AppUser(String email, String phoneE164, String passwordHash) {
        this.emailNormalized = email;
        this.phoneE164 = phoneE164;
        this.passwordHash = passwordHash;
        this.enabled = false;
    }

    public String getEmailNormalized() { return emailNormalized; }
    public String getPhoneE164() { return phoneE164; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public Instant getPhoneVerifiedAt() { return phoneVerifiedAt; }

    public void changePasswordHash(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) throw new IllegalArgumentException("Senha inválida.");
        passwordHash = encodedPassword;
    }

    public void verifyEmail(Instant verifiedAt) {
        if (emailVerifiedAt == null) emailVerifiedAt = verifiedAt;
        updateEnabled();
    }

    public void verifyPhone(Instant verifiedAt) {
        if (phoneVerifiedAt == null) phoneVerifiedAt = verifiedAt;
        updateEnabled();
    }

    private void updateEnabled() {
        enabled = emailVerifiedAt != null && phoneVerifiedAt != null;
    }
}

