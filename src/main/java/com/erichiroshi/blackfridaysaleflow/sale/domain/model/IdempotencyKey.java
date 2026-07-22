package com.erichiroshi.blackfridaysaleflow.sale.domain.model;

import java.util.Objects;

/**
 * Value Object for the client-supplied Idempotency-Key header.
 * Guarantees that retried HTTP requests for the same logical reservation
 * do not double-decrement stock.
 */
public record IdempotencyKey(String value) {

    public IdempotencyKey {
        Objects.requireNonNull(value, "IdempotencyKey value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("IdempotencyKey value must not be blank");
        }
    }

    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }
}
