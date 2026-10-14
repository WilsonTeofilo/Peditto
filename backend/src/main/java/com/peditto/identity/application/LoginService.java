package com.peditto.identity.application;

import com.peditto.common.persistence.AuditWriter;
import com.peditto.identity.domain.AppUser;
import com.peditto.identity.domain.AuthChallenge;
import com.peditto.identity.domain.AuthChallengePurpose;
import com.peditto.identity.domain.BrazilianDocument;
import com.peditto.identity.domain.DocumentType;
import com.peditto.identity.domain.IdentityInputNormalizer;
import com.peditto.identity.domain.LoginPreAuthentication;
import com.peditto.identity.domain.OtpChannel;
import com.peditto.identity.domain.UserDocument;
import com.peditto.identity.domain.UserSession;
import com.peditto.identity.infrastructure.AppUserRepository;
import com.peditto.identity.infrastructure.AuthChallengeRepository;
import com.peditto.identity.infrastructure.LoginPreAuthenticationRepository;
import com.peditto.identity.infrastructure.UserDocumentRepository;
import com.peditto.identity.infrastructure.UserSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Password is the first factor; this service never creates a session before a valid OTP. */
@Service
public class LoginService {
    private static final List<OtpChannel> CHANNELS = List.of(OtpChannel.EMAIL, OtpChannel.SMS);
    private static final Duration PRE_AUTH_LIFETIME = Duration.ofMinutes(5);
    private static final Duration SESSION_LIFETIME = Duration.ofHours(12);
    private final AppUserRepository users;
    private final UserDocumentRepository documents;
    private final AuthChallengeRepository challenges;
    private final LoginPreAuthenticationRepository preAuthentications;
    private final UserSessionRepository sessions;
    private final PasswordEncoder passwords;
    private final IdentityAbuseLimiter limiter;
    private final OtpDeliveryService otpDelivery;
    private final String timingHash;
    private final AuditWriter audit;

    public LoginService(AppUserRepository users, UserDocumentRepository documents, AuthChallengeRepository challenges,
            LoginPreAuthenticationRepository preAuthentications, UserSessionRepository sessions,
            PasswordEncoder passwords, IdentityAbuseLimiter limiter, OtpDeliveryService otpDelivery, AuditWriter audit) {
        this.users = users; this.documents = documents; this.challenges = challenges;
        this.preAuthentications = preAuthentications; this.sessions = sessions; this.passwords = passwords;
        this.limiter = limiter; this.otpDelivery = otpDelivery;
        this.audit = audit;
        this.timingHash = passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public LoginStart begin(String identifier, String password) {
        boolean allowed = limiter.allowLogin(normalizeIdentifier(identifier));
        if (!allowed) {
            passwords.matches(password == null ? "" : password, timingHash);
            return new LoginStart(TokenDigests.newToken(), CHANNELS, "verification_required");
        }
        AppUser candidate = resolveUser(identifier).orElse(null);
        AppUser user = candidate == null ? null : users.lockById(candidate.getId()).orElse(null);
        boolean passwordValid = user != null && passwords.matches(password == null ? "" : password, user.getPasswordHash());
        if (user == null) passwords.matches(password == null ? "" : password, timingHash);
        String rawToken = TokenDigests.newToken();
        if (passwordValid && user.isEnabled()) {
            preAuthentications.save(new LoginPreAuthentication(user, TokenDigests.sha256(rawToken), Instant.now().plus(PRE_AUTH_LIFETIME)));
        }
        // Same response shape and channel labels whether credentials are valid or not.
        return new LoginStart(rawToken, CHANNELS, "verification_required");
    }

    @Transactional
    public boolean requestCode(String preAuthToken, OtpChannel channel) {
        if (preAuthToken == null || channel == null || !CHANNELS.contains(channel)) return false;
        String tokenHash = TokenDigests.sha256(preAuthToken);
        LoginPreAuthentication candidate = preAuthentications.findByTokenHash(tokenHash).orElse(null);
        if (candidate == null) return false;
        AppUser user = users.lockById(candidate.getUser().getId()).orElse(null);
        LoginPreAuthentication preAuth = preAuthentications.lockByTokenHash(tokenHash).orElse(null);
        Instant now = Instant.now();
        if (preAuth == null || user == null || !preAuth.getUser().getId().equals(user.getId())
                || !preAuth.isUsableAt(now) || !user.isEnabled()) return false;
        if (!limiter.allowOtp(user.getEmailNormalized(), channel.name())) return false;
        String destination = channel == OtpChannel.EMAIL ? user.getEmailNormalized() : user.getPhoneE164();
        otpDelivery.issue(user, AuthChallengePurpose.LOGIN, channel, destination);
        return true;
    }

    @Transactional
    public Optional<String> verifyAndCreateSession(String preAuthToken, OtpChannel channel, String code) {
        if (preAuthToken == null || channel == null || !CHANNELS.contains(channel)) return Optional.empty();
        String tokenHash = TokenDigests.sha256(preAuthToken);
        LoginPreAuthentication candidate = preAuthentications.findByTokenHash(tokenHash).orElse(null);
        if (candidate == null) return Optional.empty();
        AppUser user = users.lockById(candidate.getUser().getId()).orElse(null);
        LoginPreAuthentication preAuth = preAuthentications.lockByTokenHash(tokenHash).orElse(null);
        Instant now = Instant.now();
        if (preAuth == null || user == null || !preAuth.getUser().getId().equals(user.getId())
                || !preAuth.isUsableAt(now) || !user.isEnabled()) return Optional.empty();
        if (!limiter.allowOtp(user.getEmailNormalized(), "LOGIN_VERIFY_" + channel.name())) return Optional.empty();
        AuthChallengePurpose purpose = AuthChallengePurpose.LOGIN;
        List<AuthChallenge> candidates = challenges.lockLiveCandidates(user.getEmailNormalized(), purpose, channel);
        AuthChallenge challenge = candidates.stream().findFirst().orElse(null);
        if (challenge == null || !challenge.isUsableAt(now) || !otpDelivery.matches(challenge, code)) {
            if (challenge != null && challenge.getAttempts() < 10) challenge.recordFailedAttempt();
            preAuth.recordFailedAttempt();
            return Optional.empty();
        }
        challenge.consume(now);
        preAuth.consume(now);
        String rawSessionToken = TokenDigests.newToken();
        UserSession session = sessions.save(new UserSession(user, UUID.randomUUID(), TokenDigests.sha256(rawSessionToken), now.plus(SESSION_LIFETIME)));
        audit.record("AUTH_SESSION_CREATED", "user_session", session.getId(), user.getId(), null);
        return Optional.of(rawSessionToken);
    }

    private Optional<AppUser> resolveUser(String identifier) {
        if (identifier == null) return Optional.empty();
        if (identifier.contains("@")) {
            try { return users.findByEmailNormalized(IdentityInputNormalizer.email(identifier)); }
            catch (IllegalArgumentException invalid) { return Optional.empty(); }
        }
        String digits = identifier.replaceAll("[.\\-/\\s]", "");
        DocumentType type = digits.length() == 11 ? DocumentType.CPF : digits.length() == 14 ? DocumentType.CNPJ : null;
        if (type == null) return Optional.empty();
        try { digits = BrazilianDocument.normalize(digits, type); }
        catch (IllegalArgumentException invalid) { return Optional.empty(); }
        return documents.findWithUserByDocument(digits).map(UserDocument::getUser);
    }

    private String normalizeIdentifier(String identifier) {
        if (identifier == null) return "invalid";
        if (identifier.contains("@")) {
            try { return "email:" + IdentityInputNormalizer.email(identifier); }
            catch (IllegalArgumentException invalid) { return "email:invalid"; }
        }
        String digits = identifier.replaceAll("[.\\-/\\s]", "");
        if (digits.length() == 11) {
            try { return "document:" + BrazilianDocument.normalize(digits, DocumentType.CPF); }
            catch (IllegalArgumentException ignored) { return "document:invalid"; }
        }
        if (digits.length() == 14) {
            try { return "document:" + BrazilianDocument.normalize(digits, DocumentType.CNPJ); }
            catch (IllegalArgumentException ignored) { return "document:invalid"; }
        }
        return "invalid";
    }

    public record LoginStart(String preAuthenticationToken, List<OtpChannel> availableChannels, String status) {
        @Override public String toString() { return "LoginStart[preAuthenticationToken=[redacted], availableChannels=" + availableChannels + ", status=" + status + "]"; }
    }
}
