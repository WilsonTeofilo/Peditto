package com.peditto.stores.infrastructure;

import com.peditto.stores.domain.StoreOpeningHour;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreOpeningHourRepository extends JpaRepository<StoreOpeningHour, UUID> {
    List<StoreOpeningHour> findAllByOrganization_IdAndStore_IdOrderByWeekdayAscSlotAsc(
            UUID organizationId, UUID storeId);

    @Modifying
    @Query("delete from StoreOpeningHour hour where hour.organization.id = :organizationId and hour.store.id = :storeId")
    int deleteForStore(@Param("organizationId") UUID organizationId, @Param("storeId") UUID storeId);
}
