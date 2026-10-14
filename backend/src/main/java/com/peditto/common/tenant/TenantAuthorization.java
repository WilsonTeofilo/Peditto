package com.peditto.common.tenant;

import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Server-side capability checks for the selected organization membership. */
@Component("tenantAuthorization")
public class TenantAuthorization {
    private static final Set<String> CAPABILITIES = Set.of(
            "catalog:read", "catalog:write", "orders:read", "orders:transition",
            "customers:read", "settings:read", "settings:write", "exports:read",
            "members:manage", "billing:read", "billing:write", "security:manage");

    private final JdbcTemplate jdbc;

    public TenantAuthorization(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean hasCapability(String capability) {
        if (capability == null || !CAPABILITIES.contains(capability)) return false;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID userId)) return false;

        var organizationId = TenantContext.requireOrganizationId();
        var roles = jdbc.query("""
                SELECT role FROM memberships
                WHERE organization_id = ? AND user_id = ? AND status = 'ACTIVE'
                """, (result, row) -> result.getString("role"), organizationId, userId);
        return roles.stream().anyMatch(role -> grants(role, capability));
    }

    public boolean canAccessProduct(UUID productId, String capability) {
        if (productId == null || capability == null || !Set.of("catalog:read", "catalog:write").contains(capability)) return false;
        UUID organizationId = TenantContext.requireOrganizationId();
        if (!hasCapability(capability)) return false;
        return exists("SELECT 1 FROM products WHERE id = ? AND organization_id = ?", productId, organizationId);
    }

    public boolean canAccessOrder(UUID orderId, String capability) {
        if (orderId == null || capability == null || !Set.of("orders:read", "orders:transition").contains(capability)) return false;
        UUID organizationId = TenantContext.requireOrganizationId();
        if (!hasCapability(capability)) return false;
        return exists("SELECT 1 FROM customer_orders WHERE id = ? AND organization_id = ?", orderId, organizationId);
    }

    private boolean exists(String sql, UUID resourceId, UUID organizationId) {
        return !jdbc.query(sql, (result, row) -> result.getInt(1), resourceId, organizationId).isEmpty();
    }



    private boolean grants(String role, String capability) {
        return switch (role) {
            case "OWNER" -> CAPABILITIES.contains(capability);
            case "MANAGER" -> switch (capability) {
                case "catalog:read", "catalog:write", "orders:read", "orders:transition",
                        "customers:read", "settings:read", "exports:read" -> true;
                default -> false;
            };
            case "STAFF" -> switch (capability) {
                case "catalog:read", "orders:read", "orders:transition" -> true;
                default -> false;
            };
            default -> false;
        };
    }
}


