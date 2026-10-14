package com.peditto.stores.infrastructure;

import com.peditto.stores.domain.Store;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, UUID> {
    boolean existsBySlug(String slug);
    Optional<Store> findByOrganization_IdAndId(UUID organizationId, UUID storeId);
    Optional<Store> findByOrganization_Id(UUID organizationId);
}
