package com.peditto.stores.application.assets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AssetStorageProperties {
    private final boolean enabled;
    private final String privateDirectory;
    private final String signingKeyBase64;

    public AssetStorageProperties(
            @Value("${peditto.assets.enabled:false}") boolean enabled,
            @Value("${peditto.assets.private-directory:./var/peditto-private-assets}") String privateDirectory,
            @Value("${peditto.assets.signing-key-base64:}") String signingKeyBase64) {
        this.enabled = enabled;
        this.privateDirectory = privateDirectory;
        this.signingKeyBase64 = signingKeyBase64;
    }

    public boolean enabled() { return enabled; }
    public String privateDirectory() { return privateDirectory; }
    public String signingKeyBase64() { return signingKeyBase64; }
}
