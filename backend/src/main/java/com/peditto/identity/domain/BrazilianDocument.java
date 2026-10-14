package com.peditto.identity.domain;

import java.util.regex.Pattern;

/** Server-side validation and normalization for Brazilian CPF/CNPJ identifiers. */
public final class BrazilianDocument {
    private static final Pattern DIGITS = Pattern.compile("^[0-9]+$");
    private BrazilianDocument() {}

    public static String normalize(String input, DocumentType type) {
        if (input == null || type == null) throw new IllegalArgumentException("Documento inválido.");
        String normalized = input.replaceAll("[.\\-/\\s]", "");
        int expected = type == DocumentType.CPF ? 11 : 14;
        if (normalized.length() != expected || !DIGITS.matcher(normalized).matches() || repeated(normalized)
                || !(type == DocumentType.CPF ? validCpf(normalized) : validCnpj(normalized))) {
            throw new IllegalArgumentException("Documento inválido.");
        }
        return normalized;
    }

    private static boolean repeated(String value) {
        return value.chars().distinct().count() == 1;
    }

    private static boolean validCpf(String cpf) {
        int first = digit(cpf.substring(0, 9), new int[]{10,9,8,7,6,5,4,3,2});
        int second = digit(cpf.substring(0, 9) + first, new int[]{11,10,9,8,7,6,5,4,3,2});
        return cpf.charAt(9) - '0' == first && cpf.charAt(10) - '0' == second;
    }

    private static boolean validCnpj(String cnpj) {
        int first = digit(cnpj.substring(0, 12), new int[]{5,4,3,2,9,8,7,6,5,4,3,2});
        int second = digit(cnpj.substring(0, 12) + first, new int[]{6,5,4,3,2,9,8,7,6,5,4,3,2});
        return cnpj.charAt(12) - '0' == first && cnpj.charAt(13) - '0' == second;
    }

    private static int digit(String value, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) sum += (value.charAt(i) - '0') * weights[i];
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
