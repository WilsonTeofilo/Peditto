package com.peditto.stores.infrastructure;

import com.peditto.stores.domain.StoreAsset;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreAssetRepository extends JpaRepository<StoreAsset, UUID> {}
