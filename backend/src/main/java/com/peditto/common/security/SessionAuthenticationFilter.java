package com.peditto.common.security;

import com.peditto.identity.application.TokenDigests;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Resolves opaque cookie sessions against PostgreSQL; client claims are never authority. */
@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    public static final String COOKIE_NAME = "__Host-PEDITTO_SESSION";
    private static final String SESSION_LOOKUP = """
            SELECT s.id, s.user_id
            FROM user_sessions s JOIN app_users u ON u.id=s.user_id
            WHERE s.refresh_token_hash=? AND s.revoked_at IS NULL AND s.expires_at > now() AND u.enabled=true
            """;
    private final JdbcTemplate jdbc;

    public SessionAuthenticationFilter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = sessionCookie(request);
            if (token != null) {
                List<SessionRow> rows = jdbc.query(SESSION_LOOKUP,
                        (rs, rowNum) -> new SessionRow(UUID.fromString(rs.getString("id")), UUID.fromString(rs.getString("user_id"))),
                        TokenDigests.sha256(token));
                if (!rows.isEmpty()) {
                    SessionRow session = rows.getFirst();
                    jdbc.update("""
                            UPDATE user_sessions
                            SET last_used_at = now(), updated_at = now()
                            WHERE id = ?
                              AND (last_used_at IS NULL OR last_used_at < now() - interval '5 minutes')
                            """, session.sessionId());
                    var authentication = new UsernamePasswordAuthenticationToken(session.userId(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        chain.doFilter(request, response);
    }

    private String sessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName()) && isValidToken(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private boolean isValidToken(String value) {
        return value != null && value.length() <= 100 && value.matches("[A-Za-z0-9_-]{40,50}");
    }

    private record SessionRow(UUID sessionId, UUID userId) {}
}
