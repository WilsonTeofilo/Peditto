package com.peditto.common.tenant;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Activates a tenant created or verified inside an unauthenticated onboarding transaction. */
@Component
public class TenantTransactionContext {
    private final JdbcTemplate jdbc;

    public TenantTransactionContext(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void activate(UUID organizationId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Tenant context must be activated inside a transaction.");
        }
        TenantContext.setOrganizationId(organizationId);
        jdbc.queryForObject(
                "SELECT set_config('peditto.organization_id', ?, true)",
                String.class,
                organizationId.toString());
    }
}
