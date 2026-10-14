package com.peditto.subscriptions.infrastructure;

import com.peditto.subscriptions.domain.Trial;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrialRepository extends JpaRepository<Trial, UUID> {
    boolean existsByOrganization_Id(UUID organizationId);
}
