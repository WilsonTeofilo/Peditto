package com.peditto.identity.api;

import com.peditto.identity.domain.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Explicit public registration allow-list. Privileged fields cannot be bound from JSON. */
public record RegistrationRequest(
        @NotNull DocumentType documentType,
        @NotBlank @Size(max = 18) String document,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$") String phoneE164,
        @NotBlank @Size(min = 12, max = 128) String password,
        @NotBlank @Size(min = 2, max = 160) String legalName,
        @NotBlank @Size(min = 2, max = 120) String storeName
) {
    @Override public String toString() {
        return "RegistrationRequest[documentType=" + documentType
                + ", document=[redacted], email=[redacted], phoneE164=[redacted]"
                + ", password=[redacted], legalName=[redacted], storeName=[redacted]]";
    }
}
