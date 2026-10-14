package com.peditto.subscriptions.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;

@Entity
@Table(name="plans")
public class Plan extends BaseEntity {
    @Column(name="code",nullable=false,length=40)
    private String code;
    @Column(nullable=false,length=100)
    private String name;
    @Column(nullable=false)
    private boolean active;
    @Version
    @Column(nullable=false)
    private long version;
    protected Plan() {}
}

