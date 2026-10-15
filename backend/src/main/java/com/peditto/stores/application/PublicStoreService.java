package com.peditto.stores.application;

import com.peditto.stores.api.DeliveryCheckResponse;
import com.peditto.stores.api.PublicStoreResponse;
import com.peditto.stores.domain.DeliveryCoverage;
import com.peditto.stores.infrastructure.PublicStoreLookupRepository;
import com.peditto.stores.infrastructure.PublicStoreLookupRepository.PublicStoreSnapshot;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PublicStoreService {
    private final PublicStoreLookupRepository stores;

    public PublicStoreService(PublicStoreLookupRepository stores) {
        this.stores = stores;
    }

    @Transactional(readOnly = true)
    public PublicStoreResponse getStore(String slug) {
        PublicStoreSnapshot store = findPublicStore(slug);
        boolean openNow = store.isOpenNow();
        return new PublicStoreResponse(store.slug(), store.displayName(), store.timezone(), openNow,
                store.deliveryFee(), store.centerLatitude(), store.centerLongitude(), store.radiusMeters(),
                openNow && store.deliveryActive());
    }

    @Transactional(readOnly = true)
    public DeliveryCheckResponse checkDelivery(String slug, BigDecimal latitude, BigDecimal longitude) {
        PublicStoreSnapshot store = findPublicStore(slug);
        boolean inside = store.isOpenNow() && store.deliveryActive() && store.centerLatitude() != null
                && DeliveryCoverage.includes(store.centerLatitude(), store.centerLongitude(), latitude, longitude,
                        store.radiusMeters());
        return new DeliveryCheckResponse(inside);
    }

    private PublicStoreSnapshot findPublicStore(String slug) {
        return stores.findBySlug(slug).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }
}
