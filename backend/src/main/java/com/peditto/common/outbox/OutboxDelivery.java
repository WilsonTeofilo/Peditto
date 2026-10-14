package com.peditto.common.outbox;

import java.util.UUID;

public record OutboxDelivery(UUID id, String eventType, String destination, byte[] ciphertext,
        String encryptionKeyVersion, short attempt) {}
