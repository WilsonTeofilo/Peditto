package com.peditto.catalog.application;

import com.peditto.catalog.api.CategoryRequest;
import com.peditto.catalog.api.CategoryResponse;
import com.peditto.catalog.domain.CatalogCategory;
import com.peditto.catalog.infrastructure.CatalogCategoryRepository;
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
public class CategoryService {
    private final CatalogAccess access;
    private final CatalogCategoryRepository categories;
    private final Clock clock;

    public CategoryService(CatalogAccess access, CatalogCategoryRepository categories, Clock clock) {
        this.access = access;
        this.categories = categories;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        access.requireCapability("catalog:read");
        UUID organizationId = access.organizationId();
        Store store = access.currentStore();
        return categories.findAllByOrganization_IdAndStore_IdOrderByDisplayOrderAscIdAsc(organizationId, store.getId())
                .stream().map(CategoryService::response).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        access.requireCapability("catalog:write");
        Store store = access.currentStore();
        CatalogCategory category = new CatalogCategory(store.getOrganization(), store,
                request.name(), request.displayOrder());
        CatalogCategory saved = categories.saveAndFlush(category);
        access.audit("CATALOG_CATEGORY_CREATED", "catalog_category", saved.getId());
        return response(saved);
    }

    @Transactional
    public CategoryResponse update(UUID categoryId, CategoryRequest request) {
        access.requireCapability("catalog:write");
        CatalogCategory category = findCategory(categoryId);
        category.rename(request.name());
        category.moveTo(request.displayOrder());
        CatalogCategory saved = categories.saveAndFlush(category);
        access.audit("CATALOG_CATEGORY_UPDATED", "catalog_category", saved.getId());
        return response(saved);
    }

    @Transactional
    public void archive(UUID categoryId) {
        access.requireCapability("catalog:write");
        CatalogCategory category = findCategory(categoryId);
        category.archive(Instant.now(clock));
        categories.saveAndFlush(category);
        access.audit("CATALOG_CATEGORY_ARCHIVED", "catalog_category", category.getId());
    }

    @Transactional
    public CategoryResponse restore(UUID categoryId) {
        access.requireCapability("catalog:write");
        CatalogCategory category = findCategory(categoryId);
        category.restore();
        CatalogCategory saved = categories.saveAndFlush(category);
        access.audit("CATALOG_CATEGORY_RESTORED", "catalog_category", saved.getId());
        return response(saved);
    }

    private CatalogCategory findCategory(UUID categoryId) {
        return categories.findByOrganization_IdAndStore_IdAndId(
                access.organizationId(), access.currentStore().getId(), categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static CategoryResponse response(CatalogCategory category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDisplayOrder(),
                category.isActive(), category.getVersion());
    }
}
