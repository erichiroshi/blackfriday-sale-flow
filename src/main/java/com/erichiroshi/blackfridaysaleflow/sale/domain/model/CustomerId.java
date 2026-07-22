package com.erichiroshi.blackfridaysaleflow.sale.domain.model;

import java.util.Objects;

/**
 * Value Object for the customer placing the order.
 */
public record CustomerId(String value) {

    public CustomerId {
        Objects.requireNonNull(value, "CustomerId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CustomerId value must not be blank");
        }
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
}
