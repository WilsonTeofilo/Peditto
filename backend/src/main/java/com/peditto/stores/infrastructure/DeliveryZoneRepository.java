package com.peditto.stores.infrastructure;

import com.peditto.stores.domain.DeliveryZone;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, UUID> {
    Optional<DeliveryZone> findByOrganization_IdAndStore_Id(UUID organizationId, UUID storeId);
}
