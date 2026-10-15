package com.peditto.stores.infrastructure;

import com.peditto.stores.domain.StoreDomain;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreDomainRepository extends JpaRepository<StoreDomain, UUID> {
    List<StoreDomain> findAllByOrganization_IdAndStore_Id(UUID organizationId, UUID storeId);
}
