package com.peditto.common.outbox;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.peditto.identity.application.OtpCryptography;
import com.peditto.identity.domain.OtpChannel;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OtpOutboxWorker {
    private static final Logger log = LoggerFactory.getLogger(OtpOutboxWorker.class);
    private final OutboxClaimService claims;
    private final OtpCryptography cryptography;
    private final OtpMessageGateway gateway;
    private final ObjectMapper mapper;
    private final boolean enabled;

    public OtpOutboxWorker(OutboxClaimService claims, OtpCryptography cryptography, OtpMessageGateway gateway,
            ObjectMapper mapper, @Value("${peditto.identity.delivery-enabled:false}") boolean enabled) {
        this.claims = claims;
        this.cryptography = cryptography;
        this.gateway = gateway;
        this.mapper = mapper;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${peditto.identity.outbox-poll-ms:1000}")
    public void deliverPending() {
        if (!enabled) return;
        for (OutboxDelivery delivery : claims.claimBatch()) deliver(delivery);
    }

    private void deliver(OutboxDelivery delivery) {
        try {
            JsonNode payload = mapper.readTree(cryptography.decrypt(delivery.ciphertext(), delivery.encryptionKeyVersion()));
            String code = required(payload, "code");
            Instant expiresAt = Instant.parse(required(payload, "expiresAt"));
            if (!expiresAt.isAfter(Instant.now())) {
                claims.markExpired(delivery);
                return;
            }
            OtpChannel channel = switch (delivery.eventType()) {
                case "IDENTITY_OTP_EMAIL" -> OtpChannel.EMAIL;
                case "IDENTITY_OTP_SMS" -> OtpChannel.SMS;
                default -> throw new IllegalArgumentException("unsupported_outbox_event");
            };
            gateway.send(channel, delivery.destination(), code);
            claims.markSent(delivery.id());
        } catch (Exception failure) {
            String errorCode = safeErrorCode(failure);
            claims.markFailed(delivery, errorCode);
            log.warn("OTP outbox delivery failed; messageId={}, errorCode={}", delivery.id(), errorCode);
        }
    }

    private String required(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.isTextual() || value.textValue().length() > 40) {
            throw new IllegalArgumentException("invalid_outbox_payload");
        }
        return value.textValue();
    }

    private String safeErrorCode(Exception failure) {
        String message = failure.getMessage();
        return message != null && message.matches("[a-z0-9_]{1,60}") ? message : "delivery_failed";
    }
}

