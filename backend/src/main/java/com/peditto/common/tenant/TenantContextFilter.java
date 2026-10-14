package com.peditto.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Validates the optional organization selector against the authenticated user's active memberships. */
@Component
public class TenantContextFilter extends OncePerRequestFilter {
    private static final String ORGANIZATION_HEADER = "X-Organization-Id";
    private static final String MEMBERSHIP_LOOKUP = """
            SELECT 1 FROM memberships
            WHERE organization_id = ? AND user_id = ? AND status = 'ACTIVE'
            LIMIT 1
            """;

    private final JdbcTemplate jdbc;

    public TenantContextFilter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        TenantContext.clear();
        try {
            String selectedOrganization = request.getHeader(ORGANIZATION_HEADER);
            if (selectedOrganization == null || selectedOrganization.isBlank()) {
                chain.doFilter(request, response);
                return;
            }

            UUID organizationId = parseOrganizationId(selectedOrganization);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID userId)
                    || !hasActiveMembership(organizationId, userId)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            TenantContext.setOrganizationId(organizationId);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private UUID parseOrganizationId(String value) {
        if (!value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean hasActiveMembership(UUID organizationId, UUID userId) {
        if (organizationId == null) return false;
        List<Integer> matches = jdbc.query(MEMBERSHIP_LOOKUP,
                (result, row) -> result.getInt(1), organizationId, userId);
        return !matches.isEmpty();
    }
}

