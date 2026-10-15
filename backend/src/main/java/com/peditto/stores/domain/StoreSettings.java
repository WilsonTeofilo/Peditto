package com.peditto.stores.domain;

import com.peditto.common.persistence.BaseEntity;
import com.peditto.organizations.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "store_settings",
indexes = @Index(name = "ix_store_settings_tenant", columnList = "organization_id,store_id"),
uniqueConstraints = @UniqueConstraint(name = "uk_store_settings_store_id", columnNames = "store_id"))
public class StoreSettings extends BaseEntity {
    private static final BigDecimal MIN_LATITUDE = new BigDecimal(StorePolicy.MIN_LATITUDE);
    private static final BigDecimal MAX_LATITUDE = new BigDecimal(StorePolicy.MAX_LATITUDE);
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal(StorePolicy.MIN_LONGITUDE);
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal(StorePolicy.MAX_LONGITUDE);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false, updatable = false, unique = true)
    private Store store;

    @Column(length = StorePolicy.MAX_STREET_LENGTH)
    private String street;
    @Column(name = "street_number", length = StorePolicy.MAX_STREET_NUMBER_LENGTH)
    private String streetNumber;
    @Column(name = "address_complement", length = StorePolicy.MAX_ADDRESS_COMPLEMENT_LENGTH)
    private String addressComplement;
    @Column(length = StorePolicy.MAX_NEIGHBORHOOD_LENGTH)
    private String neighborhood;
    @Column(length = StorePolicy.MAX_CITY_LENGTH)
    private String city;
    @Column(name = "state_code", length = StorePolicy.STATE_CODE_LENGTH)
    private String stateCode;
    @Column(name = "postal_code", length = StorePolicy.POSTAL_CODE_LENGTH)
    private String postalCode;
    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;
    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;
    @Column(name = "accepts_pix", nullable = false)
    private boolean acceptsPix = true;
    @Column(name = "accepts_card", nullable = false)
    private boolean acceptsCard;
    @Column(name = "accepts_cash_on_delivery", nullable = false)
    private boolean acceptsCashOnDelivery;
    @Column(name = "delivery_fee", nullable = false,
            precision = StorePolicy.DELIVERY_FEE_PRECISION, scale = StorePolicy.DELIVERY_FEE_SCALE)
    private BigDecimal deliveryFee = BigDecimal.ZERO;
    @Column(name = "preparation_minutes", nullable = false)
    private int preparationMinutes = 30;
    @Version
    @Column(nullable = false)
    private long version;
    protected StoreSettings() {}

    public StoreSettings(Organization organization, Store store) {
        this.organization = organization;
        this.store = store;
    }

    public void replaceAddressAndPayments(String street, String streetNumber, String addressComplement,
            String neighborhood, String city, String stateCode, String postalCode,
            BigDecimal latitude, BigDecimal longitude, boolean acceptsPix, boolean acceptsCard,
            boolean acceptsCashOnDelivery, BigDecimal deliveryFee, int preparationMinutes) {
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("Latitude e longitude devem ser informadas juntas.");
        }
        if (latitude != null && (latitude.scale() > StorePolicy.COORDINATE_DECIMAL_PLACES
                || longitude.scale() > StorePolicy.COORDINATE_DECIMAL_PLACES
                || latitude.compareTo(MIN_LATITUDE) < 0
                || latitude.compareTo(MAX_LATITUDE) > 0
                || longitude.compareTo(MIN_LONGITUDE) < 0
                || longitude.compareTo(MAX_LONGITUDE) > 0)) {
            throw new IllegalArgumentException("Coordenadas fora dos limites permitidos.");
        }
        if (!acceptsPix && !acceptsCard && !acceptsCashOnDelivery) {
            throw new IllegalArgumentException("A loja precisa aceitar ao menos uma forma de pagamento.");
        }
        if (deliveryFee == null || deliveryFee.signum() < 0 || preparationMinutes < 0
                || preparationMinutes > StorePolicy.MAX_PREPARATION_MINUTES) {
            throw new IllegalArgumentException("Taxa ou tempo de preparo inválido.");
        }
        BigDecimal normalizedFee;
        try {
            normalizedFee = deliveryFee.setScale(StorePolicy.DELIVERY_FEE_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("A taxa deve ter no máximo duas casas decimais.");
        }
        if (normalizedFee.precision() > StorePolicy.DELIVERY_FEE_PRECISION) {
            throw new IllegalArgumentException("A taxa de entrega excede o limite permitido.");
        }

        this.street = street;
        this.streetNumber = streetNumber;
        this.addressComplement = addressComplement;
        this.neighborhood = neighborhood;
        this.city = city;
        this.stateCode = stateCode;
        this.postalCode = postalCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.acceptsPix = acceptsPix;
        this.acceptsCard = acceptsCard;
        this.acceptsCashOnDelivery = acceptsCashOnDelivery;
        this.deliveryFee = normalizedFee;
        this.preparationMinutes = preparationMinutes;
    }

    public UUID getStoreId() { return store.getId(); }
    public String getStreet() { return street; }
    public String getStreetNumber() { return streetNumber; }
    public String getAddressComplement() { return addressComplement; }
    public String getNeighborhood() { return neighborhood; }
    public String getCity() { return city; }
    public String getStateCode() { return stateCode; }
    public String getPostalCode() { return postalCode; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public boolean acceptsPix() { return acceptsPix; }
    public boolean acceptsCard() { return acceptsCard; }
    public boolean acceptsCashOnDelivery() { return acceptsCashOnDelivery; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public int getPreparationMinutes() { return preparationMinutes; }
    public long getVersion() { return version; }
}

