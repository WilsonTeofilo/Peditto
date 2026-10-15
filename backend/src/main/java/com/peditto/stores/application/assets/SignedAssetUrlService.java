package com.peditto.stores.application.assets;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class SignedAssetUrlService {
    private static final Duration URL_LIFETIME = Duration.ofMinutes(10);
    private final AssetStorageProperties properties;
    private final Clock clock;

    public SignedAssetUrlService(AssetStorageProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String create(UUID assetId) {
        try {
            byte[] key = Base64.getDecoder().decode(properties.signingKeyBase64());
            if (key.length < 32) throw new IllegalStateException("Asset signing key must contain at least 256 bits.");
            long expiresAt = Instant.now(clock).plus(URL_LIFETIME).getEpochSecond();
            String message = assetId + ":" + expiresAt;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            String signature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
            return "/api/v1/public/assets/" + assetId + "?expiresAt=" + expiresAt + "&signature=" + signature;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Asset signing key must be configured as Base64.", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign asset URL.", exception);
        }
    }

    public boolean isValid(UUID assetId, long expiresAt, String signature) {
        long now = Instant.now(clock).getEpochSecond();
        if (signature == null || signature.isBlank()
                || expiresAt < now || expiresAt > now + URL_LIFETIME.toSeconds()) return false;
        String expectedUrl = createForExpiry(assetId, expiresAt);
        return expectedUrl != null && MessageDigest.isEqual(expectedUrl.getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII));
    }

    private String createForExpiry(UUID assetId, long expiresAt) {
        try {
            byte[] key = Base64.getDecoder().decode(properties.signingKeyBase64());
            if (key.length < 32) return null;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal((assetId + ":" + expiresAt).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            return null;
        }
    }
}
