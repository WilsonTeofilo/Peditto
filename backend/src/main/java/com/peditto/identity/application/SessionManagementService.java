package com.peditto.identity.application;

import com.peditto.common.persistence.AuditWriter;
import com.peditto.identity.domain.UserSession;
import com.peditto.identity.infrastructure.AppUserRepository;
import com.peditto.identity.infrastructure.UserSessionRepository;
import java.time.Instant;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionManagementService {
    private static final Duration SESSION_LIFETIME = Duration.ofHours(12);
    private final UserSessionRepository sessions;
    private final AppUserRepository users;
    private final AuditWriter audit;
    public SessionManagementService(UserSessionRepository sessions, AppUserRepository users, AuditWriter audit) {
        this.sessions = sessions; this.users = users; this.audit = audit;
    }

    @Transactional
    public boolean revokeCurrent(String rawToken) {
        if (rawToken == null) return false;
        UserSession session = sessions.lockByTokenHash(TokenDigests.sha256(rawToken)).orElse(null);
        if (session == null || session.getRevokedAt() != null) return false;
        session.revoke(Instant.now(), "USER_LOGOUT");
        audit.record("AUTH_SESSION_REVOKED", "user_session", session.getId(), session.getUser().getId(), null);
        return true;
    }

    @Transactional(readOnly = true)
    public java.util.List<SessionSummary> listActive(UUID userId, String currentToken) {
        String currentHash = currentToken == null ? null : TokenDigests.sha256(currentToken);
        return sessions.findAllByUser_IdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(userId, Instant.now())
                .stream()
                .map(session -> new SessionSummary(session.getId(), session.getCreatedAt(), session.getLastUsedAt(),
                        session.getExpiresAt(), currentHash != null && session.matchesTokenHash(currentHash)))
                .toList();
    }

    @Transactional
    public SessionRevocation revoke(UUID userId, UUID sessionId, String currentToken) {
        UserSession session = sessions.lockByIdAndUserId(sessionId, userId).orElse(null);
        if (session == null || session.getRevokedAt() != null) return new SessionRevocation(false, false);
        boolean current = currentToken != null
                && session.matchesTokenHash(TokenDigests.sha256(currentToken));
        session.revoke(Instant.now(), "USER_REVOKED");
        audit.record("AUTH_SESSION_REVOKED", "user_session", session.getId(), userId, null);
        return new SessionRevocation(true, current);
    }

    @Transactional
    public int revokeAll(UUID authenticatedUserId) {
        if (users.lockById(authenticatedUserId).isEmpty()) return 0;
        int count = 0;
        for (UserSession session : sessions.findAllByUser_IdAndRevokedAtIsNull(authenticatedUserId)) {
            session.revoke(Instant.now(), "GLOBAL_LOGOUT");
            audit.record("AUTH_SESSION_REVOKED", "user_session", session.getId(), authenticatedUserId, null);
            count++;
        }
        return count;
    }

    /** Rotates an opaque browser session. Reuse of a revoked token revokes its remaining family. */
    @Transactional
    public Optional<String> rotate(String rawToken) {
        if (rawToken == null) return Optional.empty();
        String tokenHash = TokenDigests.sha256(rawToken);
        UserSession candidate = sessions.findByRefreshTokenHash(tokenHash).orElse(null);
        if (candidate == null || users.lockById(candidate.getUser().getId()).isEmpty()) return Optional.empty();
        UserSession current = sessions.lockByTokenHash(tokenHash).orElse(null);
        Instant now = Instant.now();
        if (current == null) return Optional.empty();
        if (current.getRevokedAt() != null) {
            int revoked = 0;
            for (UserSession active : sessions.lockActiveByFamily(current.getFamilyId())) {
                active.revoke(now, "TOKEN_REUSE");
                audit.record("AUTH_SESSION_REVOKED", "user_session", active.getId(), active.getUser().getId(), null);
                revoked++;
            }
            if (revoked > 0) audit.record("AUTH_SESSION_REUSE_DETECTED", "user_session", current.getId(), current.getUser().getId(), null);
            return Optional.empty();
        }
        if (current.isExpiredAt(now) || !current.getUser().isEnabled()) return Optional.empty();
        String replacementToken = TokenDigests.newToken();
        UserSession replacement = sessions.save(new UserSession(current.getUser(), current.getFamilyId(),
                TokenDigests.sha256(replacementToken), now.plus(SESSION_LIFETIME)));
        current.markReplacedBy(replacement.getId());
        current.revoke(now, "ROTATED");
        audit.record("AUTH_SESSION_ROTATED", "user_session", replacement.getId(), current.getUser().getId(), null);
        return Optional.of(replacementToken);
    }

    public record SessionSummary(UUID id, Instant createdAt, Instant lastUsedAt, Instant expiresAt, boolean current) {}
    public record SessionRevocation(boolean revoked, boolean current) {}
}
