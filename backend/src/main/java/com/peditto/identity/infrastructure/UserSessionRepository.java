package com.peditto.identity.infrastructure;

import com.peditto.identity.domain.UserSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    java.util.Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);
    java.util.List<UserSession> findAllByUser_IdAndRevokedAtIsNull(UUID userId);
    java.util.List<UserSession> findAllByUser_IdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(UUID userId, Instant now);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserSession s where s.id=:sessionId and s.user.id=:userId")
    Optional<UserSession> lockByIdAndUserId(@Param("sessionId") UUID sessionId, @Param("userId") UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserSession s join fetch s.user where s.familyId=:familyId and s.revokedAt is null")
    java.util.List<UserSession> lockActiveByFamily(@Param("familyId") UUID familyId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserSession s join fetch s.user where s.refreshTokenHash=:hash")
    Optional<UserSession> lockByTokenHash(@Param("hash") String hash);
}
