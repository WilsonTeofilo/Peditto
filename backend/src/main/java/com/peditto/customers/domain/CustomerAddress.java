package com.peditto.customers.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import com.peditto.stores.domain.Store;
import com.peditto.customers.domain.CustomerProfile;

@Entity
@Table(name="customer_addresses")
public class CustomerAddress extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="organization_id",nullable=false)
    private Organization organization;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="store_id",nullable=false)
    private Store store;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="customer_id",nullable=false)
    private CustomerProfile customer;
    @Column(name="recipient_name",nullable=false,length=120)
    private String recipientName;
    @Column(nullable=false,length=180)
    private String street;
    @Column(name="street_number",nullable=false,length=30)
    private String streetNumber;
    @Column(name="neighborhood",nullable=false,length=120)
    private String neighborhood;
    @Column(nullable=false,length=120)
    private String city;
    @Column(name="state_code",nullable=false,length=2)
    private String stateCode;
    @Column(name="latitude",precision=9,scale=6)
    private BigDecimal latitude;
    @Column(name="longitude",precision=9,scale=6)
    private BigDecimal longitude;
    @Column(name="is_default",nullable=false)
    private boolean defaultAddress;
    @Version
    @Column(nullable=false)
    private long version;
    protected CustomerAddress() {}
}

