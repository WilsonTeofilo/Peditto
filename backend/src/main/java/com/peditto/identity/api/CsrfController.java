package com.peditto.identity.api;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/csrf")
public class CsrfController {
    @GetMapping
    public CsrfResponse token(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    public record CsrfResponse(String headerName, String token) {}
}
