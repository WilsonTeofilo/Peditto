package com.peditto.orders.infrastructure;

import com.peditto.orders.domain.CustomerOrder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Every management query requires both tenant and store identifiers. */
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {
    Optional<CustomerOrder> findByOrganization_IdAndStore_IdAndId(UUID organizationId, UUID storeId, UUID id);
    Page<CustomerOrder> findAllByOrganization_IdAndStore_Id(UUID organizationId, UUID storeId, Pageable pageable);
}
