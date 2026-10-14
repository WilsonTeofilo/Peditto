package com.peditto.identity.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** Canonical forms accepted by the identity application service. */
public final class IdentityInputNormalizer {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]{1,64}@[^\\s@.]+(?:\\.[^\\s@.]+)+$");
    private static final Pattern PHONE = Pattern.compile("^\\+[1-9][0-9]{7,14}$");
    private IdentityInputNormalizer() {}

    public static String email(String value) {
        if (value == null) throw new IllegalArgumentException("E-mail inválido.");
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !EMAIL.matcher(normalized).matches()) throw new IllegalArgumentException("E-mail inválido.");
        return normalized;
    }

    /** Accept E.164 only; presentation formatting is handled before this API boundary. */
    public static String phoneE164(String value) {
        if (value == null || !PHONE.matcher(value).matches()) throw new IllegalArgumentException("Telefone inválido.");
        return value;
    }

    public static String requiredDisplayName(String value, int maxLength) {
        if (value == null) throw new IllegalArgumentException("Nome inválido.");
        String normalized = value.strip().replaceAll("\\s+", " ");
        if (normalized.isBlank() || normalized.length() > maxLength || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Nome inválido.");
        }
        return normalized;
    }
}
