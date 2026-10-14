package com.peditto.common.outbox;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OutboxClaimService {
    private static final int MAX_ATTEMPTS = 8;
    private final JdbcTemplate jdbc;

    public OutboxClaimService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public List<OutboxDelivery> claimBatch() {
        return jdbc.query("""
                WITH claimable AS (
                    SELECT id
                    FROM outbox_messages
                    WHERE (status = 'PENDING' AND available_at <= now())
                       OR (status = 'PROCESSING' AND locked_until <= now())
                    ORDER BY available_at, created_at
                    FOR UPDATE SKIP LOCKED
                    LIMIT 20
                )
                UPDATE outbox_messages AS message
                SET status = 'PROCESSING', locked_until = now() + interval '2 minutes',
                    attempts = attempts + 1, updated_at = now()
                FROM claimable
                WHERE message.id = claimable.id
                RETURNING message.id, message.event_type, message.destination,
                          message.payload_ciphertext, message.encryption_key_version, message.attempts
                """, (result, row) -> new OutboxDelivery(
                result.getObject("id", UUID.class), result.getString("event_type"),
                result.getString("destination"), result.getBytes("payload_ciphertext"),
                result.getString("encryption_key_version"), result.getShort("attempts")));
    }

    public void markSent(UUID id) {
        jdbc.update("""
                UPDATE outbox_messages
                SET status = 'SENT', sent_at = now(), locked_until = NULL,
                    destination = '[sent]', payload_ciphertext = NULL, updated_at = now()
                WHERE id = ? AND status = 'PROCESSING'
                """, id);
    }

    public void markFailed(OutboxDelivery delivery, String errorCode) {
        boolean dead = delivery.attempt() >= MAX_ATTEMPTS;
        long delaySeconds = Math.min(Duration.ofMinutes(15).toSeconds(), 1L << Math.min(delivery.attempt(), 9));
        jdbc.update("""
                UPDATE outbox_messages
                SET status = ?, available_at = now() + (? * interval '1 second'),
                    locked_until = NULL, last_error_code = ?,
                    destination = CASE WHEN ? THEN '[dead]' ELSE destination END,
                    updated_at = now()
                WHERE id = ? AND status = 'PROCESSING'
                """, dead ? "DEAD" : "PENDING", delaySeconds, errorCode, dead, delivery.id());
    }

    public void markExpired(OutboxDelivery delivery) {
        jdbc.update("""
                UPDATE outbox_messages
                SET status = 'DEAD', locked_until = NULL, last_error_code = 'otp_expired',
                    destination = '[expired]', updated_at = now()
                WHERE id = ? AND status = 'PROCESSING'
                """, delivery.id());
    }
}
