package com.peditto.identity.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.peditto.identity.api.RegistrationRequest;
import com.peditto.identity.domain.DocumentType;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class RegistrationFacadeTest {
    @Mock private RegistrationService registration;
    @Mock private IdentityAbuseLimiter limiter;
    @InjectMocks private RegistrationFacade facade;

    private RegistrationInput input;

    @BeforeEach
    void setUp() {
        input = RegistrationInput.from(new RegistrationRequest(
                DocumentType.CPF, "529.982.247-25", "owner@example.test", "+5511999999999",
                "a-long-test-password", "Loja de teste", "Loja de teste"));
    }

    @Test
    void duplicateIdentityReturnsTheSameGenericAcceptance() {
        allowSignup(true);
        var duplicate = new DataIntegrityViolationException("duplicate", new SQLException("duplicate", "23505"));
        doThrow(duplicate).when(registration).register(input);

        assertEquals("verification_required", facade.register(input).status());
    }

    @Test
    void unexpectedIntegrityFailureIsNotHiddenAsAcceptedSignup() {
        allowSignup(true);
        var failure = new DataIntegrityViolationException("invalid reference", new SQLException("invalid reference", "23503"));
        doThrow(failure).when(registration).register(input);

        assertSame(failure, assertThrows(DataIntegrityViolationException.class, () -> facade.register(input)));
    }

    @Test
    void throttledSignupDoesNotStartRegistration() {
        allowSignup(false);

        assertEquals("verification_required", facade.register(input).status());
        verify(registration, never()).register(any());
    }

    private void allowSignup(boolean allowed) {
        when(limiter.allowSignup(input.email(), input.phoneE164(), input.document())).thenReturn(allowed);
    }
}
