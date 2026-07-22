package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.database;

/**
 * Persistence-layer mirror of {@link com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderStatus}.
 * Kept as a distinct type so a future divergence between the domain
 * lifecycle and the stored representation doesn't ripple into the domain.
 */
public enum OrderStatusJpa {
    RESERVED,
    CONFIRMED,
    FAILED
}
