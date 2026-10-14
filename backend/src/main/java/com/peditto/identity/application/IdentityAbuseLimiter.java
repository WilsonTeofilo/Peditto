package com.peditto.identity.application;

import com.peditto.common.security.RateLimitBucketStore;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Distributed destination throttles. Only keyed fingerprints, never raw contacts/documents, reach PostgreSQL. */
@Component
public class IdentityAbuseLimiter {
    private final RateLimitBucketStore buckets;
    private final byte[] key;

    public IdentityAbuseLimiter(RateLimitBucketStore buckets,
            @Value("${peditto.security.rate-limit.hmac-key}") String key) {
        this.buckets = buckets;
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    public boolean allowSignup(String email, String phone, String document) {
        boolean emailAllowed = allow("signup-email", email, 3600, 3, 60);
        boolean phoneAllowed = allow("signup-phone", phone, 3600, 3, 60);
        boolean documentAllowed = allow("signup-document", document, 3600, 3, 60);
        return emailAllowed && phoneAllowed && documentAllowed;
    }

    public boolean allowOtp(String email, String channel) {
        return allow("otp-verify-" + channel, email, 900, 10, 15);
    }

    public boolean allowLogin(String identifier) {
        return allow("login-identifier", identifier, 900, 10, 15);
    }

    private boolean allow(String namespace, String value, int windowSeconds, int maxRequests, int blockMinutes) {
        return !buckets.isBlocked(fingerprint(namespace + ":" + value), windowSeconds, maxRequests, blockMinutes);
    }

    private String fingerprint(String material) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(material.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException impossible) { throw new IllegalStateException("Rate limit fingerprint unavailable.", impossible); }
    }
}
