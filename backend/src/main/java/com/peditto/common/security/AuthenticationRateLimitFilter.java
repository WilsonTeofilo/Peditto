package com.peditto.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationRateLimitFilter.class);
    private final RateLimitBucketStore buckets;
    private final byte[] hmacKey;
    private final int maxRequests;
    private final int windowSeconds;
    private final int blockMinutes;

    public AuthenticationRateLimitFilter(RateLimitBucketStore buckets,
            @Value("${peditto.security.rate-limit.hmac-key}") String hmacKey,
            @Value("${peditto.security.rate-limit.auth-max-requests:10}") int maxRequests,
            @Value("${peditto.security.rate-limit.auth-window-seconds:60}") int windowSeconds,
            @Value("${peditto.security.rate-limit.auth-block-minutes:15}") int blockMinutes) {
        this.buckets = buckets;
        this.hmacKey = hmacKey.getBytes(StandardCharsets.UTF_8);
        if (this.hmacKey.length < 32) throw new IllegalStateException("RATE_LIMIT_HMAC_KEY must contain at least 32 bytes.");
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.blockMinutes = blockMinutes;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !(path.startsWith("/auth/") || path.startsWith("/api/v1/auth/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String key = fingerprint(request.getRemoteAddr());
            if (buckets.isBlocked(key, windowSeconds, maxRequests, blockMinutes)) {
                response.setHeader("Retry-After", Integer.toString(blockMinutes * 60));
                response.setStatus(429);
                response.setContentType("application/problem+json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                writeProblem(response, 429, "Muitas tentativas", "Tente novamente mais tarde.", request);
                return;
            }
        } catch (GeneralSecurityException | DataAccessException exception) {
            log.error("Authentication abuse control unavailable; requestId={}, exceptionType={}",
                    request.getAttribute(com.peditto.common.api.RequestIdFilter.ATTRIBUTE), exception.getClass().getName());
            writeProblem(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Serviço temporariamente indisponível", "Tente novamente mais tarde.", request);
            return;
        }
        chain.doFilter(request, response);
    }

    private String fingerprint(String remoteAddress) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(("auth-ip:" + remoteAddress).getBytes(StandardCharsets.UTF_8)));
    }

    private void writeProblem(HttpServletResponse response, int status, String title, String detail,
            HttpServletRequest request) throws IOException {
        Object requestId = request.getAttribute(com.peditto.common.api.RequestIdFilter.ATTRIBUTE);
        String safeRequestId = requestId instanceof String value ? value : "unavailable";
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title
                + "\",\"status\":" + status + ",\"detail\":\"" + detail
                + "\",\"requestId\":\"" + safeRequestId + "\"}");
    }
}

