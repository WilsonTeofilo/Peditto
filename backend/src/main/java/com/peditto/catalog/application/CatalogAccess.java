package com.peditto.catalog.application;

import com.peditto.common.persistence.AuditWriter;
import com.peditto.common.tenant.TenantAuthorization;
import com.peditto.common.tenant.TenantContext;
import com.peditto.stores.domain.Store;
import com.peditto.stores.infrastructure.StoreRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CatalogAccess {
    private final StoreRepository stores;
    private final TenantAuthorization authorization;
    private final AuditWriter audit;

    public CatalogAccess(StoreRepository stores, TenantAuthorization authorization, AuditWriter audit) {
        this.stores = stores;
        this.authorization = authorization;
        this.audit = audit;
    }

    public UUID organizationId() {
        return TenantContext.requireOrganizationId();
    }

    public Store currentStore() {
        return stores.findByOrganization_Id(organizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public void requireCapability(String capability) {
        if (!authorization.hasCapability(capability)) {
            throw new AccessDeniedException("A capability is required for this catalog operation.");
        }
    }

    public void audit(String action, String resourceType, UUID resourceId) {
        audit.record(action, resourceType, resourceId, actorId(), organizationId());
    }

    public UUID actorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID actorId)) {
            throw new AccessDeniedException("An authenticated store member is required.");
        }
        return actorId;
    }
}
