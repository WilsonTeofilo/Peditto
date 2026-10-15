package com.peditto.stores.domain;

/** Shared operational limits for store setup; database checks mirror these values. */
public final class StorePolicy {
    public static final int MAX_STORE_SLUG_LENGTH = 63;
    public static final int MAX_STORE_NAME_LENGTH = 120;
    public static final int MAX_TIMEZONE_LENGTH = 64;
    public static final int MAX_STREET_LENGTH = 180;
    public static final int MAX_STREET_NUMBER_LENGTH = 30;
    public static final int MAX_ADDRESS_COMPLEMENT_LENGTH = 120;
    public static final int MAX_NEIGHBORHOOD_LENGTH = 120;
    public static final int MAX_CITY_LENGTH = 120;
    public static final int STATE_CODE_LENGTH = 2;
    public static final int POSTAL_CODE_LENGTH = 9;
    public static final int MAX_PREPARATION_MINUTES = 1_440;
    public static final int DELIVERY_FEE_PRECISION = 10;
    public static final int DELIVERY_FEE_SCALE = 2;
    public static final String MIN_LATITUDE = "-90";
    public static final String MAX_LATITUDE = "90";
    public static final String MIN_LONGITUDE = "-180";
    public static final String MAX_LONGITUDE = "180";
    public static final int MIN_DELIVERY_RADIUS_METERS = 100;
    public static final int MAX_DELIVERY_RADIUS_METERS = 100_000;
    public static final int COORDINATE_DECIMAL_PLACES = 6;
    public static final int DAYS_PER_WEEK = 7;
    public static final int MAX_INTERVALS_PER_DAY = 4;
    public static final int MAX_INTERVALS_PER_WEEK = DAYS_PER_WEEK * MAX_INTERVALS_PER_DAY;

    private StorePolicy() {}
}
