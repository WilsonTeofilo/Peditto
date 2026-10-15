package com.peditto.catalog.application;

import com.peditto.catalog.api.ProductAddonRequest;
import com.peditto.catalog.api.ProductAddonResponse;
import com.peditto.catalog.domain.Product;
import com.peditto.catalog.domain.ProductAddon;
import com.peditto.catalog.infrastructure.ProductAddonRepository;
import com.peditto.catalog.infrastructure.ProductRepository;
import com.peditto.stores.domain.Store;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductAddonService {
    private final CatalogAccess access;
    private final ProductRepository products;
    private final ProductAddonRepository addons;
    private final Clock clock;

    public ProductAddonService(CatalogAccess access, ProductRepository products,
            ProductAddonRepository addons, Clock clock) {
        this.access = access;
        this.products = products;
        this.addons = addons;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ProductAddonResponse> list(UUID productId) {
        access.requireCapability("catalog:read");
        Product product = findProduct(productId);
        return addons.findAllByOrganization_IdAndStore_IdAndProduct_IdOrderByNameAsc(
                        access.organizationId(), product.getStoreId(), product.getId())
                .stream().map(ProductAddonService::response).toList();
    }

    @Transactional
    public ProductAddonResponse create(UUID productId, ProductAddonRequest request) {
        access.requireCapability("catalog:write");
        Store store = access.currentStore();
        Product product = findProduct(productId);
        ProductAddon addon = new ProductAddon(store.getOrganization(), store, product,
                request.name(), request.description(), request.priceDelta(), request.maxQuantity());
        ProductAddon saved = addons.saveAndFlush(addon);
        access.audit("CATALOG_ADDON_CREATED", "product_addon", saved.getId());
        return response(saved);
    }

    @Transactional
    public ProductAddonResponse update(UUID productId, UUID addonId, ProductAddonRequest request) {
        access.requireCapability("catalog:write");
        Product product = findProduct(productId);
        ProductAddon addon = findAddon(product, addonId);
        addon.update(request.name(), request.description(), request.priceDelta(), request.maxQuantity());
        ProductAddon saved = addons.saveAndFlush(addon);
        access.audit("CATALOG_ADDON_UPDATED", "product_addon", saved.getId());
        return response(saved);
    }

    @Transactional
    public void archive(UUID productId, UUID addonId) {
        access.requireCapability("catalog:write");
        Product product = findProduct(productId);
        ProductAddon addon = findAddon(product, addonId);
        addon.archive(Instant.now(clock));
        addons.saveAndFlush(addon);
        access.audit("CATALOG_ADDON_ARCHIVED", "product_addon", addon.getId());
    }

    @Transactional
    public ProductAddonResponse restore(UUID productId, UUID addonId) {
        access.requireCapability("catalog:write");
        Product product = findProduct(productId);
        ProductAddon addon = findAddon(product, addonId);
        addon.restore();
        ProductAddon saved = addons.saveAndFlush(addon);
        access.audit("CATALOG_ADDON_RESTORED", "product_addon", saved.getId());
        return response(saved);
    }

    private Product findProduct(UUID productId) {
        return products.findByOrganization_IdAndStore_IdAndId(
                access.organizationId(), access.currentStore().getId(), productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private ProductAddon findAddon(Product product, UUID addonId) {
        return addons.findByOrganization_IdAndStore_IdAndProduct_IdAndId(
                access.organizationId(), product.getStoreId(), product.getId(), addonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static ProductAddonResponse response(ProductAddon addon) {
        return new ProductAddonResponse(addon.getId(), addon.getProductId(), addon.getName(),
                addon.getDescription(), addon.getPriceDelta(), addon.getMaxQuantity(),
                addon.isActive(), addon.getVersion());
    }
}
