package com.peditto.stores.application.assets;

import com.peditto.catalog.application.CatalogAccess;
import com.peditto.catalog.domain.Product;
import com.peditto.catalog.infrastructure.ProductRepository;
import com.peditto.stores.domain.Store;
import com.peditto.stores.domain.StoreAsset;
import com.peditto.stores.infrastructure.StoreAssetRepository;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductAssetService {
    private final CatalogAccess access;
    private final ProductRepository products;
    private final StoreAssetRepository assets;
    private final ImageUploadValidator validator;
    private final PrivateObjectStorage storage;
    private final SignedAssetUrlService urls;
    private final AssetStorageProperties properties;
    private final Clock clock;

    public ProductAssetService(CatalogAccess access, ProductRepository products, StoreAssetRepository assets,
            ImageUploadValidator validator, PrivateObjectStorage storage, SignedAssetUrlService urls,
            AssetStorageProperties properties, Clock clock) {
        this.access = access;
        this.products = products;
        this.assets = assets;
        this.validator = validator;
        this.storage = storage;
        this.urls = urls;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public AssetUploadResult uploadProductImage(UUID productId, String filename,
            String declaredType, InputStream content) {
        if (!properties.enabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Envio de imagens indisponível.");
        }
        access.requireCapability("catalog:write");
        Store store = access.currentStore();
        Product product = products.findByOrganization_IdAndStore_IdAndId(
                access.organizationId(), store.getId(), productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        SanitizedImage image = validator.validate(filename, declaredType, content);
        String objectKey = UUID.randomUUID() + ".png";
        byte[] bytes = image.bytes();
        storage.store(objectKey, bytes);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) storage.delete(objectKey);
            }
        });

        StoreAsset asset = new StoreAsset(store.getOrganization(), store, product, objectKey,
                image.mediaType(), bytes.length, sha256(bytes), image.width(), image.height());
        StoreAsset saved = assets.saveAndFlush(asset);
        access.audit("CATALOG_PRODUCT_IMAGE_UPLOADED", "store_asset", saved.getId());
        return new AssetUploadResult(saved.getId(), urls.create(saved.getId()));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
