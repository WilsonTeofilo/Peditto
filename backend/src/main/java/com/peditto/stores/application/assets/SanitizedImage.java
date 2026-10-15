package com.peditto.stores.application.assets;

public record SanitizedImage(byte[] bytes, int width, int height, String mediaType) {
    public SanitizedImage { bytes = bytes.clone(); }
    @Override public byte[] bytes() { return bytes.clone(); }
}
