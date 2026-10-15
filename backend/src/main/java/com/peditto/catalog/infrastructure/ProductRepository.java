package com.peditto.catalog.infrastructure;

import com.peditto.catalog.domain.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findAllByOrganization_IdAndStore_Id(UUID organizationId, UUID storeId, Pageable pageable);

    Optional<Product> findByOrganization_IdAndStore_IdAndId(
            UUID organizationId, UUID storeId, UUID productId);
}
