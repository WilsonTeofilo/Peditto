package com.peditto.stores.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import com.peditto.stores.domain.Store;

@Entity
@Table(name="store_assets")
public class StoreAsset extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="organization_id",nullable=false)
    private Organization organization;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="store_id",nullable=false)
    private Store store;
    @Column(name="object_key",nullable=false,length=300)
    private String objectKey;
    @Column(name="media_type",nullable=false,length=80)
    private String mediaType;
    @Column(name="byte_size",nullable=false)
    private long byteSize;
    @Column(nullable=false,length=64)
    private String sha256;
    private Integer width;
    private Integer height;
    @Column(nullable=false,length=16)
    private String status;
    @Version
    @Column(nullable=false)
    private long version;
    protected StoreAsset() {}
}

