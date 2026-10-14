package com.peditto.subscriptions.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import tools.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.peditto.subscriptions.domain.Plan;

@Entity
@Table(name="plan_price_versions")
public class PlanPriceVersion extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="plan_id",nullable=false)
    private Plan plan;
    @Column(nullable=false,precision=12,scale=2)
    private BigDecimal amount;
    @Column(nullable=false,length=3)
    private String currency;
    @Column(name="interval_unit",nullable=false,length=12)
    private String intervalUnit;
    @Column(name="effective_from",nullable=false)
    private Instant effectiveFrom;
    @Column(name="effective_until")
    private Instant effectiveUntil;
    @Column(name="trial_days",nullable=false)
    private short trialDays;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable=false,columnDefinition="jsonb")
    private JsonNode entitlements;
    @Version
    @Column(nullable=false)
    private long version;
    protected PlanPriceVersion() {}
}

