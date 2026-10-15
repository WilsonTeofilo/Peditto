package com.peditto.catalog.infrastructure;

import com.peditto.catalog.domain.CatalogCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogCategoryRepository extends JpaRepository<CatalogCategory, UUID> {
    List<CatalogCategory> findAllByOrganization_IdAndStore_IdOrderByDisplayOrderAscIdAsc(
            UUID organizationId, UUID storeId);

    Optional<CatalogCategory> findByOrganization_IdAndStore_IdAndId(
            UUID organizationId, UUID storeId, UUID categoryId);
}
