package com.peditto.identity.application;

import java.sql.SQLException;
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
        catch (DataIntegrityViolationException conflict) {
            if (!isUniqueConstraintViolation(conflict)) throw conflict;
        }
        return new RegistrationResult("verification_required");
    }

    static boolean isUniqueConstraintViolation(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && "23505".equals(sql.getSQLState())) return true;
        }
        return false;
    }

    public record RegistrationResult(String status) {}
}
