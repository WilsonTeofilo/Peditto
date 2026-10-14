package com.peditto.identity.infrastructure;

import com.peditto.identity.domain.LoginPreAuthentication;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginPreAuthenticationRepository extends JpaRepository<LoginPreAuthentication, UUID> {
    Optional<LoginPreAuthentication> findByTokenHash(String tokenHash);

    @org.springframework.data.jpa.repository.Modifying
    @Query("update LoginPreAuthentication p set p.consumedAt = CURRENT_TIMESTAMP where p.user.id = :userId and p.consumedAt is null")
    int consumeAllForUser(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from LoginPreAuthentication p join fetch p.user where p.tokenHash=:hash")
    Optional<LoginPreAuthentication> lockByTokenHash(@Param("hash") String hash);
}
