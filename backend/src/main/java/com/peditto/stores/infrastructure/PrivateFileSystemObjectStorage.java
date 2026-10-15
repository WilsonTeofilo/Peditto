package com.peditto.stores.infrastructure;

import com.peditto.stores.application.assets.AssetStorageProperties;
import com.peditto.stores.application.assets.PrivateObjectStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.stereotype.Component;

@Component
public class PrivateFileSystemObjectStorage implements PrivateObjectStorage {
    private final Path root;

    public PrivateFileSystemObjectStorage(AssetStorageProperties properties) {
        this.root = Path.of(properties.privateDirectory()).toAbsolutePath().normalize();
        Path staticRoot = Path.of("src", "main", "resources", "static").toAbsolutePath().normalize();
        if (root.startsWith(staticRoot)) {
            throw new IllegalArgumentException("Private asset storage cannot be inside the static web root.");
        }
    }

    @Override
    public void store(String key, byte[] content) {
        Path path = resolve(key);
        try {
            Files.createDirectories(root);
            Files.write(path, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store private asset.", exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not remove uncommitted private asset.", exception);
        }
    }

    private Path resolve(String key) {
        if (key == null || !key.matches("[0-9a-f-]{36}\\.png")) {
            throw new IllegalArgumentException("Invalid private asset key.");
        }
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) throw new IllegalArgumentException("Invalid private asset path.");
        return resolved;
    }
}
