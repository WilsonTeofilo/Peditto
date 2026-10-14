package com.peditto.identity.application;

import com.peditto.common.persistence.AuditWriter;
import com.peditto.identity.domain.AppUser;
import com.peditto.identity.domain.AuthChallenge;
import com.peditto.identity.domain.AuthChallengePurpose;
import com.peditto.identity.domain.IdentityInputNormalizer;
import com.peditto.identity.domain.OtpChannel;
import com.peditto.identity.infrastructure.AppUserRepository;
import com.peditto.identity.infrastructure.AuthChallengeRepository;
import com.peditto.identity.infrastructure.LoginPreAuthenticationRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Recovery requires valid one-time codes delivered to both previously verified contacts. */
@Service
public class PasswordRecoveryService {
    private final AppUserRepository users;
    private final AuthChallengeRepository challenges;
    private final OtpDeliveryService otpDelivery;
    private final IdentityAbuseLimiter limiter;
    private final PasswordEncoder passwords;
    private final SessionManagementService sessions;
    private final LoginPreAuthenticationRepository preAuthentications;
    private final AuditWriter audit;

    public PasswordRecoveryService(AppUserRepository users, AuthChallengeRepository challenges,
            OtpDeliveryService otpDelivery, IdentityAbuseLimiter limiter, PasswordEncoder passwords,
            SessionManagementService sessions, LoginPreAuthenticationRepository preAuthentications, AuditWriter audit) {
        this.users = users;
        this.challenges = challenges;
        this.otpDelivery = otpDelivery;
        this.limiter = limiter;
        this.passwords = passwords;
        this.sessions = sessions;
        this.preAuthentications = preAuthentications;
        this.audit = audit;
    }

    @Transactional
    public void request(String email) {
        final String normalized;
        try { normalized = IdentityInputNormalizer.email(email); }
        catch (IllegalArgumentException invalid) { return; }
        if (!limiter.allowOtp(normalized, "PASSWORD_RESET_REQUEST")) return;
        AppUser user = users.findByEmailNormalized(normalized).orElse(null);
        if (user == null || !user.isEnabled() || user.getEmailVerifiedAt() == null || user.getPhoneVerifiedAt() == null) return;
        otpDelivery.issue(user, AuthChallengePurpose.PASSWORD_RESET, OtpChannel.EMAIL, user.getEmailNormalized());
        otpDelivery.issue(user, AuthChallengePurpose.PASSWORD_RESET, OtpChannel.SMS, user.getPhoneE164());
    }

    @Transactional
    public boolean reset(String email, String emailCode, String smsCode, String newPassword) {
        final String normalized;
        try { normalized = IdentityInputNormalizer.email(email); }
        catch (IllegalArgumentException invalid) { return false; }
        if (!validPassword(newPassword) || !limiter.allowOtp(normalized, "PASSWORD_RESET_VERIFY")) return false;
        AppUser user = users.lockByEmailNormalized(normalized).orElse(null);
        if (user == null || !user.isEnabled()) return false;

        List<AuthChallenge> emailChallenges = challenges.lockLiveCandidates(normalized,
                AuthChallengePurpose.PASSWORD_RESET, OtpChannel.EMAIL);
        List<AuthChallenge> smsChallenges = challenges.lockLiveCandidates(normalized,
                AuthChallengePurpose.PASSWORD_RESET, OtpChannel.SMS);
        Instant now = Instant.now();
        AuthChallenge emailChallenge = emailChallenges.stream().findFirst().orElse(null);
        AuthChallenge smsChallenge = smsChallenges.stream().findFirst().orElse(null);
        boolean emailValid = emailChallenge != null && emailChallenge.isUsableAt(now)
                && otpDelivery.matches(emailChallenge, emailCode);
        boolean smsValid = smsChallenge != null && smsChallenge.isUsableAt(now)
                && otpDelivery.matches(smsChallenge, smsCode);
        if (!emailValid || !smsValid) {
            if (emailChallenge != null && emailChallenge.getAttempts() < 10) emailChallenge.recordFailedAttempt();
            if (smsChallenge != null && smsChallenge.getAttempts() < 10) smsChallenge.recordFailedAttempt();
            return false;
        }

        emailChallenge.consume(now);
        smsChallenge.consume(now);
        user.changePasswordHash(passwords.encode(newPassword));
        preAuthentications.consumeAllForUser(user.getId());
        sessions.revokeAll(user.getId());
        audit.record("AUTH_PASSWORD_RECOVERED", "app_user", user.getId(), user.getId(), null);
        return true;
    }

    private boolean validPassword(String password) {
        return password != null && password.length() >= 12 && password.length() <= 128
                && password.codePoints().noneMatch(Character::isISOControl);
    }
}
