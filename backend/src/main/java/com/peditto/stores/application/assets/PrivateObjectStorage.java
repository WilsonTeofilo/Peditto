package com.peditto.stores.application.assets;

public interface PrivateObjectStorage {
    void store(String key, byte[] content);
    void delete(String key);
}
