package com.peditto.catalog.infrastructure;

import com.peditto.catalog.domain.ProductPriceHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, UUID> {}
