package com.peditto.orders.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.identity.domain.AppUser;
import com.peditto.organizations.domain.Organization;
import com.peditto.stores.domain.Store;
import com.peditto.orders.domain.CustomerOrder;

@Entity
@Table(name="order_events")
public class OrderEvent extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="organization_id",nullable=false)
    private Organization organization;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="store_id",nullable=false)
    private Store store;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="order_id",nullable=false)
    private CustomerOrder order;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="actor_user_id")
    private AppUser actor;
    @Column(name="from_status",length=24)
    private String fromStatus;
    @Column(name="to_status",nullable=false,length=24)
    private String toStatus;
    @Column(name="request_id",length=80)
    private String requestId;
    protected OrderEvent() {}
}

