package com.erichiroshi.blackfridaysaleflow.sale.domain.model;

/**
 * Lifecycle of an order in the async flow.
 *
 * RESERVED  -> stock was decremented in Redis, message published to RabbitMQ,
 *              row not yet persisted in Postgres.
 * CONFIRMED -> batch worker successfully persisted the order.
 * FAILED    -> batch worker exhausted retries; Redis stock was compensated (INCR).
 */
public enum OrderStatus {
    RESERVED,
    CONFIRMED,
    FAILED
}
