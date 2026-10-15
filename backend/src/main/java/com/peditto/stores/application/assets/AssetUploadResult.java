package com.peditto.stores.application.assets;

import java.util.UUID;

public record AssetUploadResult(UUID id, String signedUrl) {}
