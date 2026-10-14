package com.peditto.catalog.infrastructure;

import com.peditto.catalog.domain.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Every product query requires both tenant and store identifiers. */
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByOrganization_IdAndStore_IdAndId(UUID organizationId, UUID storeId, UUID id);
    Page<Product> findAllByOrganization_IdAndStore_IdAndArchivedAtIsNull(UUID organizationId, UUID storeId, Pageable pageable);
}
