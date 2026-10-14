package com.peditto.identity.application;

import com.peditto.common.security.SessionAuthenticationFilter;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class SessionCookieFactory {
    private static final Duration MAX_AGE = Duration.ofHours(12);
    public ResponseCookie issue(String opaqueToken) {
        return ResponseCookie.from(SessionAuthenticationFilter.COOKIE_NAME, opaqueToken)
                .httpOnly(true).secure(true).sameSite("Lax").path("/").maxAge(MAX_AGE).build();
    }
    public ResponseCookie clear() {
        return ResponseCookie.from(SessionAuthenticationFilter.COOKIE_NAME, "")
                .httpOnly(true).secure(true).sameSite("Lax").path("/").maxAge(Duration.ZERO).build();
    }
}
