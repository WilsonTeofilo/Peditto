package com.peditto.organizations.infrastructure;
import com.peditto.organizations.domain.Membership;
import com.peditto.organizations.domain.MembershipRole;
import com.peditto.organizations.domain.MembershipStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    Optional<Membership> findByOrganization_IdAndRoleAndStatus(UUID organizationId, MembershipRole role, MembershipStatus status);
    Optional<Membership> findByUser_IdAndRoleAndStatus(UUID userId, MembershipRole role, MembershipStatus status);
}
