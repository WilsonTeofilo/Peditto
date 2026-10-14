package com.peditto.identity.application;

import com.peditto.common.persistence.OutboxMessage;
import com.peditto.common.persistence.OutboxMessageRepository;
import com.peditto.identity.domain.AppUser;
import com.peditto.identity.domain.AuthChallenge;
import com.peditto.identity.domain.AuthChallengePurpose;
import com.peditto.identity.domain.OtpChannel;
import com.peditto.identity.infrastructure.AuthChallengeRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OtpDeliveryService {
    private static final Duration LIFETIME = Duration.ofMinutes(10);
    private final AuthChallengeRepository challenges;
    private final OutboxMessageRepository outbox;
    private final OtpCryptography crypto;

    public OtpDeliveryService(AuthChallengeRepository challenges, OutboxMessageRepository outbox, OtpCryptography crypto) {
        this.challenges = challenges; this.outbox = outbox; this.crypto = crypto;
    }

    public void issue(AppUser user, AuthChallengePurpose purpose, OtpChannel channel, String destination) {
        challenges.consumePrevious(user.getId(), purpose, channel, Instant.now());
        String code = crypto.newCode();
        Instant expires = Instant.now().plus(LIFETIME);
        String hash = crypto.codeMac(user.getId().toString(), purpose.name(), channel.name(), code, expires.getEpochSecond());
        AuthChallenge challenge = challenges.saveAndFlush(new AuthChallenge(user, purpose, channel, hash, expires));
        String event = channel == OtpChannel.EMAIL ? "IDENTITY_OTP_EMAIL" : "IDENTITY_OTP_SMS";
        String payload = "{\"code\":\"" + code + "\",\"purpose\":\"" + purpose.name() + "\",\"expiresAt\":\"" + expires + "\"}";
        OtpCryptography.EncryptedPayload encrypted = crypto.encrypt(payload);
        outbox.save(new OutboxMessage(user.getId(), event, destination, encrypted.ciphertext(), encrypted.keyVersion(),
                "otp:" + UUID.randomUUID()));
    }

    public boolean matches(AuthChallenge challenge, String code) {
        return crypto.matches(challenge.getCodeHash(), challenge.getUser().getId().toString(), challenge.getPurpose().name(),
                challenge.getChannel().name(), code, challenge.getExpiresAt().getEpochSecond());
    }
}
