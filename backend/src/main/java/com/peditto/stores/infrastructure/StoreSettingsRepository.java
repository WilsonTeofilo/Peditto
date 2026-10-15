package com.peditto.stores.infrastructure;

import com.peditto.stores.domain.StoreSettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreSettingsRepository extends JpaRepository<StoreSettings, UUID> {
    Optional<StoreSettings> findByOrganization_IdAndStore_Id(UUID organizationId, UUID storeId);
}
