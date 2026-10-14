package com.peditto.common.persistence;

import tools.jackson.databind.JsonNode;
import com.peditto.identity.domain.AppUser;
import com.peditto.organizations.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "ix_audit_events_org_created", columnList = "organization_id,created_at"),
    @Index(name = "ix_audit_events_actor_created", columnList = "actor_user_id,created_at")
    })
public class AuditEvent extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private AppUser actor;

    @Column(nullable = false, length = 80)
    private String action;
    @Column(name = "resource_type", nullable = false, length = 80)
    private String resourceType;
    @Column(name = "resource_id")
    private UUID resourceId;
    @Column(name = "request_id", length = 80)
    private String requestId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_redacted", nullable = false, columnDefinition = "jsonb")
    private JsonNode metadataRedacted;

    protected AuditEvent() {}

    public AuditEvent(Organization organization, AppUser actor, String action, String resourceType,
    UUID resourceId, String requestId, JsonNode redactedMetadata) {
        this.organization = organization;
        this.actor = actor;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.requestId = requestId;
        this.metadataRedacted = redactedMetadata;
    }

    public String getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public UUID getResourceId() { return resourceId; }
    public String getRequestId() { return requestId; }
}


