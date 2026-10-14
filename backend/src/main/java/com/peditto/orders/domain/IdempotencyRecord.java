package com.peditto.orders.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;

@Entity
@Table(name="idempotency_records")
public class IdempotencyRecord extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="organization_id",nullable=false)
    private Organization organization;
    @Column(name="actor_fingerprint",nullable=false,length=64)
    private String actorFingerprint;
    @Column(nullable=false,length=100)
    private String operation;
    @Column(name="idempotency_key",nullable=false,length=128)
    private String idempotencyKey;
    @Column(name="request_fingerprint",nullable=false,length=64)
    private String requestFingerprint;
    @Column(nullable=false,length=16)
    private String state;
    @Column(name="resource_type",length=80)
    private String resourceType;
    @Column(name="resource_id")
    private UUID resourceId;
    @Column(name="response_status")
    private Short responseStatus;
    @Column(name="expires_at",nullable=false)
    private Instant expiresAt;
    protected IdempotencyRecord() {}
}

