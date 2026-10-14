package com.peditto.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessage extends BaseEntity {
    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 254)
    private String destination;

    @Column(name = "payload_ciphertext")
    private byte[] payloadCiphertext;

    @Column(name = "encryption_key_version", nullable = false, length = 80)
    private String encryptionKeyVersion;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false)
    private short attempts;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "last_error_code", length = 80)
    private String lastErrorCode;

    @Column(name = "deduplication_key", nullable = false, length = 160)
    private String deduplicationKey;

    protected OutboxMessage() {}

    public OutboxMessage(UUID aggregateId, String eventType, String destination, byte[] ciphertext,
            String keyVersion, String deduplicationKey) {
        this.aggregateType = "APP_USER";
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.destination = destination;
        this.payloadCiphertext = ciphertext;
        this.encryptionKeyVersion = keyVersion;
        this.status = "PENDING";
        this.attempts = 0;
        this.availableAt = Instant.now();
        this.deduplicationKey = deduplicationKey;
    }
}
