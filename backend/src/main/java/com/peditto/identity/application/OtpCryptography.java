package com.peditto.identity.application;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class OtpCryptography {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final byte[] hmacKey;
    private final byte[] encryptionKey;
    private final String keyVersion;

    public OtpCryptography(@Value("${peditto.identity.otp-hmac-key}") String hmacKey,
            @Value("${peditto.identity.outbox-encryption-key-base64}") String encryptionKey,
            @Value("${peditto.identity.outbox-key-version}") String keyVersion) {
        this.hmacKey = hmacKey.getBytes(StandardCharsets.UTF_8);
        if (this.hmacKey.length < 32) throw new IllegalStateException("OTP_HMAC_KEY must contain at least 32 bytes.");
        try { this.encryptionKey = Base64.getDecoder().decode(encryptionKey); }
        catch (IllegalArgumentException invalid) { throw new IllegalStateException("Outbox encryption key must be valid Base64."); }
        if (this.encryptionKey.length != 32) throw new IllegalStateException("Outbox encryption key must decode to 32 bytes.");
        if (keyVersion == null || !keyVersion.matches("[A-Za-z0-9._-]{1,80}")) throw new IllegalStateException("Outbox key version is invalid.");
        this.keyVersion = keyVersion;
    }

    public String newCode() { return "%06d".formatted(RANDOM.nextInt(1_000_000)); }

    public String codeMac(String userId, String purpose, String channel, String code, long expiresEpochSecond) {
        String material = userId + "|" + purpose + "|" + channel + "|" + expiresEpochSecond + "|" + code;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(material.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException impossible) { throw new IllegalStateException("OTP cryptography unavailable.", impossible); }
    }

    public boolean matches(String expectedHex, String userId, String purpose, String channel, String code, long expiresEpochSecond) {
        if (expectedHex == null || code == null || !code.matches("[0-9]{6}")) return false;
        byte[] expected;
        try { expected = HexFormat.of().parseHex(expectedHex); }
        catch (IllegalArgumentException invalid) { return false; }
        byte[] actual = HexFormat.of().parseHex(codeMac(userId, purpose, channel, code, expiresEpochSecond));
        return MessageDigest.isEqual(expected, actual);
    }

    public EncryptedPayload encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[12]; RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(keyVersion.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return new EncryptedPayload(ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array(), keyVersion);
        } catch (GeneralSecurityException failure) { throw new IllegalStateException("Outbox encryption unavailable.", failure); }
    }

    public String decrypt(byte[] payload, String payloadKeyVersion) {
        if (payload == null || payload.length < 29 || !keyVersion.equals(payloadKeyVersion))
            throw new IllegalStateException("Outbox payload cannot be decrypted with the configured key.");
        try {
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] nonce = new byte[12]; buffer.get(nonce);
            byte[] ciphertext = new byte[buffer.remaining()]; buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(keyVersion.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException failure) { throw new IllegalStateException("Outbox payload authentication failed.", failure); }
    }

    public record EncryptedPayload(byte[] ciphertext, String keyVersion) {}
}
