package com.peditto.catalog.infrastructure;

import com.peditto.catalog.domain.CatalogCategory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Every category query requires both tenant and store identifiers. */
public interface CatalogCategoryRepository extends JpaRepository<CatalogCategory, UUID> {
    Optional<CatalogCategory> findByOrganization_IdAndStore_IdAndId(UUID organizationId, UUID storeId, UUID id);
    Page<CatalogCategory> findAllByOrganization_IdAndStore_IdAndActiveTrue(UUID organizationId, UUID storeId, Pageable pageable);
}
