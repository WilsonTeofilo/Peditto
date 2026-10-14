package com.peditto.identity.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** Keeps duplicate identity/contact responses indistinguishable from accepted signup. */
@Service
public class RegistrationFacade {
    private final RegistrationService registration;
    private final IdentityAbuseLimiter limiter;
    public RegistrationFacade(RegistrationService registration, IdentityAbuseLimiter limiter) {
        this.registration = registration; this.limiter = limiter;
    }

    public RegistrationResult register(RegistrationInput input) {
        if (!limiter.allowSignup(input.email(), input.phoneE164(), input.document()))
            return new RegistrationResult("verification_required");
        try { registration.register(input); }
        catch (DataIntegrityViolationException duplicateOrConflict) {
            // Intentionally do not reveal whether email, phone, CPF, CNPJ or slug already exists.
        }
        return new RegistrationResult("verification_required");
    }

    public record RegistrationResult(String status) {}
}
