package com.peditto.common.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.datasource.hikari.minimum-idle=1",
        "peditto.identity.delivery-enabled=false",
        "management.health.mail.enabled=false"
})
@EnabledIfEnvironmentVariable(named = "PEDITTO_TEST_DATABASE", matches = "true")
class TenantTransactionIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private TenantTransactionContext tenantTransactionContext;
    @Autowired private TenantAuthorization tenantAuthorization;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void transactionManagerSetsAndClearsTenantOnReusedConnection() {
        UUID tenant = UUID.randomUUID();
        TenantContext.setOrganizationId(tenant);
        int backendPid = inTransaction(() -> jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
        String observedTenant = inTransaction(() -> jdbc.queryForObject(
                "SELECT current_setting('peditto.organization_id', true)", String.class));

        TenantContext.clear();
        int reusedBackendPid = inTransaction(() -> jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
        String clearedTenant = inTransaction(() -> jdbc.queryForObject(
                "SELECT current_setting('peditto.organization_id', true)", String.class));
        int visibleRowsWithoutTenant = inTransaction(() -> jdbc.queryForObject(
                "SELECT count(*) FROM catalog_categories", Integer.class));

        assertEquals(backendPid, reusedBackendPid);
        assertEquals(tenant.toString(), observedTenant);
        assertEquals("", clearedTenant);
        assertEquals(0, visibleRowsWithoutTenant);
    }

    @Test
    void onboardingCanActivateNewTenantInsideItsCurrentTransaction() {
        UUID organizationId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        String slug = "onboarding-" + storeId.toString().replace("-", "").substring(0, 20);

        TenantContext.clear();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Instant now = Instant.now();
            insertOrganization(organizationId, randomCnpjDigits(), now);
            tenantTransactionContext.activate(organizationId);
            insertStore(storeId, organizationId, slug, now);
            assertEquals(1, count("stores"));
            status.setRollbackOnly();
        });

        TenantContext.clear();
        assertTrue(jdbc.queryForList("SELECT id FROM organizations WHERE id = ?", UUID.class, organizationId).isEmpty());
    }

    @Test
    void capabilitiesComeFromTheActiveServerSideMembership() {
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContext.setOrganizationId(organizationId);
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                Instant now = Instant.now();
                insertOrganization(organizationId, randomCnpjDigits(), now);
                jdbc.update("""
                        INSERT INTO app_users(id,created_at,updated_at,email_normalized,phone_e164,password_hash,enabled)
                        VALUES (?,?,?,'roles-test@example.invalid','+5511999990000','not-a-real-hash',true)
                        """, userId, Timestamp.from(now), Timestamp.from(now));
                jdbc.update("""
                        INSERT INTO memberships(id,created_at,updated_at,organization_id,user_id,role,status)
                        VALUES (?,?,?, ?, ?, 'MANAGER', 'ACTIVE')
                        """, UUID.randomUUID(), Timestamp.from(now), Timestamp.from(now), organizationId, userId);

                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(userId, null, List.of()));
                assertTrue(tenantAuthorization.hasCapability("settings:read"));
                assertFalse(tenantAuthorization.hasCapability("settings:write"));
                assertFalse(tenantAuthorization.hasCapability("billing:write"));
                assertFalse(tenantAuthorization.hasCapability("platform:admin"));
                status.setRollbackOnly();
            });
        } finally {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
        }
    }

    @Test
    void rlsSeparatesTwoTenantFixturesAndRollsThemBack() {
        UUID organizationA = UUID.randomUUID();
        UUID organizationB = UUID.randomUUID();
        UUID storeA = UUID.randomUUID();
        UUID storeB = UUID.randomUUID();
        UUID categoryA = UUID.randomUUID();
        UUID categoryB = UUID.randomUUID();
        UUID settingsA = UUID.randomUUID();
        UUID zoneA = UUID.randomUUID();
        UUID hoursA = UUID.randomUUID();
        UUID domainA = UUID.randomUUID();
        String documentA = randomCnpjDigits();
        String documentB = randomCnpjDigits();
        String slugA = "rls-a-" + storeA.toString().replace("-", "").substring(0, 20);
        String slugB = "rls-b-" + storeB.toString().replace("-", "").substring(0, 20);

        TenantContext.setOrganizationId(organizationA);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Instant now = Instant.now();
            insertOrganization(organizationA, documentA, now);
            insertOrganization(organizationB, documentB, now);
            insertStore(storeA, organizationA, slugA, now);
            setDatabaseTenant(organizationB);
            insertStore(storeB, organizationB, slugB, now);
            setDatabaseTenant(organizationA);
            jdbc.update("UPDATE stores SET status = 'ACTIVE' WHERE id = ?", storeA);
            insertCategory(categoryA, organizationA, storeA, "tenant A", now);
            insertTenantOperationalRows(settingsA, zoneA, hoursA, domainA, organizationA, storeA, slugA, now);

            setDatabaseTenant(organizationB);
            insertCategory(categoryB, organizationB, storeB, "tenant B", now);

            setDatabaseTenant(organizationA);
            assertEquals(List.of("tenant A"), visibleCategoryNames());
            assertEquals(1, count("store_settings"));
            assertEquals(1, count("delivery_zones"));
            assertEquals(1, count("store_opening_hours"));
            assertEquals(1, count("store_domains"));
            setDatabaseTenant(organizationB);
            assertEquals(List.of("tenant B"), visibleCategoryNames());
            assertEquals(0, count("store_settings"));
            assertEquals(0, count("delivery_zones"));
            assertEquals(0, count("store_opening_hours"));
            assertEquals(0, count("store_domains"));
            assertEquals(storeA, jdbc.queryForObject(
                    "SELECT store_id FROM peditto_find_public_store_by_domain(?)", UUID.class, slugA + ".test"));
            status.setRollbackOnly();
        });

        TenantContext.clear();
        assertTrue(jdbc.queryForList("SELECT id FROM organizations WHERE id IN (?, ?)", UUID.class,
                organizationA, organizationB).isEmpty());
    }

    private <T> T inTransaction(java.util.function.Supplier<T> query) {
        return new TransactionTemplate(transactionManager).execute(status -> query.get());
    }

    private void insertOrganization(UUID id, String document, Instant now) {
        jdbc.update("""
                INSERT INTO organizations(id, created_at, updated_at, document_type, document_normalized,
                    legal_name, trade_name, status)
                VALUES (?, ?, ?, 'CNPJ', ?, 'Tenant test', 'Tenant test', 'ACTIVE')
                """, id, Timestamp.from(now), Timestamp.from(now), document);
    }

    private void insertStore(UUID id, UUID organizationId, String slug, Instant now) {
        jdbc.update("""
                INSERT INTO stores(id, created_at, updated_at, organization_id, slug, display_name, status, timezone)
                VALUES (?, ?, ?, ?, ?, 'Tenant test', 'SETUP_REQUIRED', 'America/Sao_Paulo')
                """, id, Timestamp.from(now), Timestamp.from(now), organizationId, slug);
    }

    private void insertTenantOperationalRows(UUID settingsId, UUID zoneId, UUID hoursId, UUID domainId,
            UUID organizationId, UUID storeId, String slug, Instant now) {
        Timestamp timestamp = Timestamp.from(now);
        jdbc.update("INSERT INTO store_settings(id,created_at,updated_at,organization_id,store_id) VALUES (?,?,?,?,?)",
                settingsId, timestamp, timestamp, organizationId, storeId);
        jdbc.update("""
                INSERT INTO delivery_zones(id,created_at,updated_at,organization_id,store_id,
                    center_latitude,center_longitude,radius_meters,active)
                VALUES (?,?,?,?,?,-23.55,-46.63,5000,true)
                """, zoneId, timestamp, timestamp, organizationId, storeId);
        jdbc.update("""
                INSERT INTO store_opening_hours(id,created_at,updated_at,organization_id,store_id,weekday,slot,
                    opens_at,closes_at,closed)
                VALUES (?,?,?,?,?,1,0,'09:00','18:00',false)
                """, hoursId, timestamp, timestamp, organizationId, storeId);
        jdbc.update("""
                INSERT INTO store_domains(id,created_at,updated_at,organization_id,store_id,
                    hostname_normalized,verified_at,active)
                VALUES (?,?,?,?,?,?,?,true)
                """, domainId, timestamp, timestamp, organizationId, storeId, slug + ".test", timestamp);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private void insertCategory(UUID id, UUID organizationId, UUID storeId, String name, Instant now) {
        jdbc.update("""
                INSERT INTO catalog_categories(id, created_at, updated_at, organization_id, store_id, name)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, Timestamp.from(now), Timestamp.from(now), organizationId, storeId, name);
    }

    private void setDatabaseTenant(UUID organizationId) {
        jdbc.queryForObject("SELECT set_config('peditto.organization_id', ?, true)", String.class,
                organizationId.toString());
    }

    private List<String> visibleCategoryNames() {
        return jdbc.queryForList("SELECT name FROM catalog_categories ORDER BY name", String.class);
    }

    private String randomCnpjDigits() {
        return Long.toString(ThreadLocalRandom.current().nextLong(10_000_000_000_000L, 99_999_999_999_999L));
    }
}
