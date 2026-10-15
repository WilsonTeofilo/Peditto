package com.peditto.catalog.domain;

public final class CatalogPolicy {
    public static final int MAX_PRODUCT_PAGE_SIZE = 100;
    public static final int MAX_CATEGORY_NAME_LENGTH = 100;
    public static final int MAX_PRODUCT_NAME_LENGTH = 140;
    public static final int MAX_PRODUCT_DESCRIPTION_LENGTH = 2_000;
    public static final int MAX_ADDON_NAME_LENGTH = 100;
    public static final int MAX_ADDON_DESCRIPTION_LENGTH = 500;
    public static final int MONEY_PRECISION = 12;
    public static final int MONEY_SCALE = 2;
    public static final int MAX_ADDON_QUANTITY = 20;

    private CatalogPolicy() {}
}
