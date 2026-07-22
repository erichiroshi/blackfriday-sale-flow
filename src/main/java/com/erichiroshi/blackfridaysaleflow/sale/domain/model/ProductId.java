package com.erichiroshi.blackfridaysaleflow.sale.domain.model;

import java.util.Objects;

/**
 * Value Object for a Product's identity.
 * Wrapping the raw id prevents primitive obsession and accidental
 * argument swaps (e.g. passing an OrderId where a ProductId is expected).
 */
public record ProductId(String value) {

    public ProductId {
        Objects.requireNonNull(value, "ProductId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ProductId value must not be blank");
        }
    }

    public static ProductId of(String value) {
        return new ProductId(value);
    }
}
