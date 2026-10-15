package com.peditto.catalog.application;

import org.springframework.data.domain.Sort;

public enum ProductSort {
    NAME("name"),
    PRICE("price"),
    CREATED_AT("createdAt");

    private final String property;

    ProductSort(String property) {
        this.property = property;
    }

    public Sort ascending() {
        return Sort.by(Sort.Direction.ASC, property).and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
