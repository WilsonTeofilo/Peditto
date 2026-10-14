package com.peditto.common.persistence;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Writes only allow-listed event names and empty redacted metadata; never accepts request payloads. */
@Component
public class AuditWriter {
    private final JdbcTemplate jdbc;
    public AuditWriter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void record(String action, String resourceType, UUID resourceId, UUID actorUserId, UUID organizationId) {
        if (action == null || !action.matches("[A-Z0-9_]{2,80}") || resourceType == null
                || !resourceType.matches("[a-z0-9_]{2,80}")) throw new IllegalArgumentException("Invalid audit event.");
        jdbc.update("""
                INSERT INTO audit_events(id,created_at,updated_at,organization_id,actor_user_id,action,resource_type,resource_id,metadata_redacted)
                VALUES (gen_random_uuid(),now(),now(),?,?,?, ?, ?, '{}'::jsonb)
                """, organizationId, actorUserId, action, resourceType, resourceId);
    }
}
