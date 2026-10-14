package com.peditto.identity.infrastructure;
import com.peditto.identity.domain.AuthChallenge;
import com.peditto.identity.domain.AuthChallengePurpose;
import com.peditto.identity.domain.OtpChannel;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthChallengeRepository extends JpaRepository<AuthChallenge, UUID> {
    @Modifying
    @Query("""
            update AuthChallenge c
            set c.consumedAt = :now
            where c.user.id = :userId
              and c.purpose = :purpose
              and c.channel = :channel
              and c.consumedAt is null
            """)
    int consumePrevious(@Param("userId") UUID userId, @Param("purpose") AuthChallengePurpose purpose,
            @Param("channel") OtpChannel channel, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from AuthChallenge c
            join fetch c.user u
            where u.emailNormalized = :email
              and c.purpose = :purpose
              and c.channel = :channel
              and c.consumedAt is null
              and c.expiresAt > CURRENT_TIMESTAMP
            order by c.createdAt desc
            """)
    List<AuthChallenge> lockLiveCandidates(@Param("email") String email,
            @Param("purpose") AuthChallengePurpose purpose, @Param("channel") OtpChannel channel);
}
