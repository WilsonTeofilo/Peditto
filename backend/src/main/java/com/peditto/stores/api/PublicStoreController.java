package com.peditto.stores.api;

import com.peditto.stores.domain.StorePolicy;
import com.peditto.stores.application.PublicStoreService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/public/stores/{slug}")
public class PublicStoreController {
    private final PublicStoreService stores;

    public PublicStoreController(PublicStoreService stores) {
        this.stores = stores;
    }

    @GetMapping
    public PublicStoreResponse store(@PathVariable @Size(max = StorePolicy.MAX_STORE_SLUG_LENGTH)
            @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug) {
        return stores.getStore(slug);
    }

    @GetMapping("/delivery-check")
    public DeliveryCheckResponse checkDelivery(@PathVariable @Size(max = StorePolicy.MAX_STORE_SLUG_LENGTH)
            @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
            @RequestParam @DecimalMin(StorePolicy.MIN_LATITUDE) @DecimalMax(StorePolicy.MAX_LATITUDE)
            @Digits(integer = 3, fraction = StorePolicy.COORDINATE_DECIMAL_PLACES) BigDecimal latitude,
            @RequestParam @DecimalMin(StorePolicy.MIN_LONGITUDE) @DecimalMax(StorePolicy.MAX_LONGITUDE)
            @Digits(integer = 3, fraction = StorePolicy.COORDINATE_DECIMAL_PLACES) BigDecimal longitude) {
        return stores.checkDelivery(slug, latitude, longitude);
    }
}
