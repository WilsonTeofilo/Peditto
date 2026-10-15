package com.peditto.stores.infrastructure;

import com.peditto.stores.domain.StoreStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PublicStoreLookupRepository {
    private static final String FIND_BY_SLUG = """
            SELECT slug, display_name, timezone, store_status, schedule_open,
                delivery_fee, center_latitude, center_longitude, radius_meters, delivery_active
            FROM peditto_find_public_store_by_slug_v2(?)
            """;

    private final JdbcTemplate jdbc;

    public PublicStoreLookupRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<PublicStoreSnapshot> findBySlug(String slug) {
        List<PublicStoreSnapshot> matches = jdbc.query(FIND_BY_SLUG, (result, row) ->
                new PublicStoreSnapshot(result.getString("store_status"),
                        result.getBoolean("schedule_open"),
                        result.getString("slug"), result.getString("display_name"),
                        result.getString("timezone"), result.getBigDecimal("delivery_fee"),
                        result.getBigDecimal("center_latitude"), result.getBigDecimal("center_longitude"),
                        (Integer) result.getObject("radius_meters"), result.getBoolean("delivery_active")), slug);
        return matches.stream().findFirst();
    }

    public record PublicStoreSnapshot(String status, boolean scheduleOpen,
            String slug, String displayName, String timezone, BigDecimal deliveryFee,
            BigDecimal centerLatitude, BigDecimal centerLongitude, Integer radiusMeters,
            boolean deliveryActive) {
        public boolean isOpenNow() {
            return StoreStatus.ACTIVE.name().equals(status) && scheduleOpen;
        }
    }
}
