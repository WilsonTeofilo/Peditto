package com.peditto.catalog.application;

import com.peditto.catalog.api.ProductPageResponse;
import com.peditto.catalog.api.ProductRequest;
import com.peditto.catalog.api.ProductResponse;
import com.peditto.catalog.domain.CatalogCategory;
import com.peditto.catalog.domain.CatalogPolicy;
import com.peditto.catalog.domain.Product;
import com.peditto.catalog.infrastructure.CatalogCategoryRepository;
import com.peditto.catalog.infrastructure.ProductRepository;
import com.peditto.catalog.infrastructure.ProductPriceHistoryRepository;
import com.peditto.catalog.domain.ProductPriceHistory;
import com.peditto.stores.domain.Store;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {
    private final CatalogAccess access;
    private final ProductRepository products;
    private final CatalogCategoryRepository categories;
    private final Clock clock;
    private final ProductPriceHistoryRepository priceHistory;

    public ProductService(CatalogAccess access, ProductRepository products,
            CatalogCategoryRepository categories, Clock clock, ProductPriceHistoryRepository priceHistory) {
        this.access = access;
        this.products = products;
        this.categories = categories;
        this.clock = clock;
        this.priceHistory = priceHistory;
    }

    @Transactional(readOnly = true)
    public ProductPageResponse list(int page, int size, ProductSort sort) {
        access.requireCapability("catalog:read");
        validatePage(page, size);
        Store store = access.currentStore();
        ProductSort ordering = sort == null ? ProductSort.NAME : sort;
        Page<Product> result = products.findAllByOrganization_IdAndStore_Id(
                access.organizationId(), store.getId(), PageRequest.of(page, size, ordering.ascending()));
        return new ProductPageResponse(result.getContent().stream().map(ProductService::response).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        access.requireCapability("catalog:write");
        Store store = access.currentStore();
        Product product = new Product(store.getOrganization(), store,
                findCategory(request.categoryId()), request.name(), request.description(), request.price());
        Product saved = products.saveAndFlush(product);
        priceHistory.save(new ProductPriceHistory(store.getOrganization(), store, saved,
                null, saved.getPrice(), access.actorId(), Instant.now(clock)));
        access.audit("CATALOG_PRODUCT_CREATED", "product", saved.getId());
        return response(saved);
    }

    @Transactional
    public ProductResponse update(UUID productId, ProductRequest request) {
        access.requireCapability("catalog:write");
        Product product = findProduct(productId);
        var previousPrice = product.getPrice();
        product.update(findCategory(request.categoryId()), request.name(), request.description(), request.price());
        Product saved = products.saveAndFlush(product);
        if (previousPrice.compareTo(saved.getPrice()) != 0) {
            Store store = access.currentStore();
            priceHistory.save(new ProductPriceHistory(store.getOrganization(), store,
                    saved, previousPrice, saved.getPrice(), access.actorId(), Instant.now(clock)));
        }
        access.audit("CATALOG_PRODUCT_UPDATED", "product", saved.getId());
        return response(saved);
    }

    @Transactional
    public void archive(UUID productId) {
        access.requireCapability("catalog:write");
        Product product = findProduct(productId);
        product.archive(Instant.now(clock));
        products.saveAndFlush(product);
        access.audit("CATALOG_PRODUCT_ARCHIVED", "product", product.getId());
    }

    @Transactional
    public ProductResponse restore(UUID productId) {
        access.requireCapability("catalog:write");
        Product product = findProduct(productId);
        product.restore();
        Product saved = products.saveAndFlush(product);
        access.audit("CATALOG_PRODUCT_RESTORED", "product", saved.getId());
        return response(saved);
    }

    @Transactional
    public ProductResponse setAvailability(UUID productId, boolean available) {
        access.requireCapability("catalog:write");
        Product product = findProduct(productId);
        product.setAvailableForSale(available);
        Product saved = products.saveAndFlush(product);
        access.audit(available ? "CATALOG_PRODUCT_MADE_AVAILABLE" : "CATALOG_PRODUCT_MADE_UNAVAILABLE",
                "product", saved.getId());
        return response(saved);
    }

    private CatalogCategory findCategory(UUID categoryId) {
        if (categoryId == null) return null;
        Store store = access.currentStore();
        CatalogCategory category = categories.findByOrganization_IdAndStore_IdAndId(
                access.organizationId(), store.getId(), categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!category.isActive()) {
            throw new IllegalArgumentException("Não é possível associar produto a uma categoria arquivada.");
        }
        return category;
    }

    private Product findProduct(UUID productId) {
        return products.findByOrganization_IdAndStore_IdAndId(
                access.organizationId(), access.currentStore().getId(), productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static ProductResponse response(Product product) {
        return new ProductResponse(product.getId(), product.getCategoryId(), product.getName(),
                product.getDescription(), product.getPrice(), product.isActive(),
                product.isAvailableForSale(), product.getVersion());
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > CatalogPolicy.MAX_PRODUCT_PAGE_SIZE) {
            throw new IllegalArgumentException("Paginação do catálogo inválida.");
        }
    }
}
