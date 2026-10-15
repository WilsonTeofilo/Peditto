package com.peditto.common.tenant;

import java.util.UUID;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;

/** Request-local tenant selected only after server-side membership validation. */
public final class TenantContext {
    private static final ThreadLocal<UUID> ORGANIZATION_ID = new ThreadLocal<>();

    private TenantContext() {}

    public static UUID requireOrganizationId() {
        UUID organizationId = ORGANIZATION_ID.get();
        if (organizationId == null) {
            throw new AccessDeniedException("An authorized organization context is required.");
        }
        return organizationId;
    }

    public static Optional<UUID> currentOrganizationId() {
        return Optional.ofNullable(ORGANIZATION_ID.get());
    }

    public static void setOrganizationId(UUID organizationId) { ORGANIZATION_ID.set(organizationId); }
    static void clear() { ORGANIZATION_ID.remove(); }
}
