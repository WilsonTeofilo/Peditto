package com.peditto.stores.domain;
import com.peditto.common.persistence.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="store_domains",indexes=@Index(name="ix_store_domains_store_id",columnList="store_id"))
public class StoreDomain extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="store_id",nullable=false,updatable=false)
    private Store store;
    @Column(name="hostname_normalized",nullable=false,unique=true,length=253)
    private String hostnameNormalized;
    @Column(name="verified_at")
    private Instant verifiedAt;
    @Column(nullable=false)
    private boolean active;
    @Version
    @Column(nullable=false)
    private long version;
    protected StoreDomain(){}

    public StoreDomain(Store store,String host){this.store=store;this.hostnameNormalized=host;this.active=false;}

    public Store getStore(){return store;}

    public String getHostnameNormalized(){return hostnameNormalized;}

    public boolean isActive(){return active;}
}
