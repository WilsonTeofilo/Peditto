package com.peditto.catalog.infrastructure;

import com.peditto.catalog.domain.ProductAddon;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAddonRepository extends JpaRepository<ProductAddon, UUID> {
    List<ProductAddon> findAllByOrganization_IdAndStore_IdAndProduct_IdOrderByNameAsc(
            UUID organizationId, UUID storeId, UUID productId);

    Optional<ProductAddon> findByOrganization_IdAndStore_IdAndProduct_IdAndId(
            UUID organizationId, UUID storeId, UUID productId, UUID addonId);
}
