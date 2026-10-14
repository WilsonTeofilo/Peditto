package com.peditto.subscriptions.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;

@Entity
@Table(name="provider_events")
public class ProviderEvent extends BaseEntity {
    @Column(nullable=false,length=40)
    private String provider;
    @Column(name="provider_event_id",nullable=false,length=180)
    private String providerEventId;
    @Column(name="event_type",nullable=false,length=100)
    private String eventType;
    @Column(name="payload_fingerprint",nullable=false,length=64)
    private String payloadFingerprint;
    @Column(nullable=false,length=16)
    private String status;
    @Column(name="received_at",nullable=false)
    private Instant receivedAt;
    @Column(name="processed_at")
    private Instant processedAt;
    @Column(nullable=false)
    private short attempts;
    @Column(name="resource_reference",length=180)
    private String resourceReference;
    protected ProviderEvent() {}
}

