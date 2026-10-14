package com.peditto.identity.application;

import com.peditto.identity.api.RegistrationRequest;
import com.peditto.identity.domain.BrazilianDocument;
import com.peditto.identity.domain.DocumentType;
import com.peditto.identity.domain.IdentityInputNormalizer;
import java.util.Objects;

/** Validated command; password is intentionally excluded from toString and logs. */
public final class RegistrationInput {
    private final DocumentType documentType;
    private final String document;
    private final String email;
    private final String phoneE164;
    private final String password;
    private final String legalName;
    private final String storeName;

    private RegistrationInput(DocumentType documentType, String document, String email, String phoneE164,
            String password, String legalName, String storeName) {
        this.documentType = documentType;
        this.document = document;
        this.email = email;
        this.phoneE164 = phoneE164;
        this.password = password;
        this.legalName = legalName;
        this.storeName = storeName;
    }

    public static RegistrationInput from(RegistrationRequest request) {
        Objects.requireNonNull(request, "registration request");
        String password = request.password();
        if (password == null || password.length() < 12 || password.length() > 128
                || password.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Senha inválida.");
        }
        return new RegistrationInput(request.documentType(),
                BrazilianDocument.normalize(request.document(), request.documentType()),
                IdentityInputNormalizer.email(request.email()),
                IdentityInputNormalizer.phoneE164(request.phoneE164()), password,
                IdentityInputNormalizer.requiredDisplayName(request.legalName(), 160),
                IdentityInputNormalizer.requiredDisplayName(request.storeName(), 120));
    }

    public DocumentType documentType() { return documentType; }
    public String document() { return document; }
    public String email() { return email; }
    public String phoneE164() { return phoneE164; }
    public String password() { return password; }
    public String legalName() { return legalName; }
    public String storeName() { return storeName; }

    @Override public String toString() {
        return "RegistrationInput[documentType=" + documentType + ", email=[redacted], phoneE164=[redacted], password=[redacted]]";
    }
}
