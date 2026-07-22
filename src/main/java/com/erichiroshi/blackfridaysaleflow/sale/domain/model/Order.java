package com.erichiroshi.blackfridaysaleflow.sale.domain.model;

import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.InvalidOrderStateTransitionException;

import java.time.Instant;
import java.util.Objects;

/**
 * Order aggregate root.
 *
 * Represents the full lifecycle of a single stock reservation: created the
 * moment the Redis reservation succeeds (RESERVED), and later transitioned
 * to CONFIRMED or FAILED by the batch worker once Postgres persistence is
 * attempted. No Spring, no persistence annotations here — this class is
 * pure business state and behavior.
 */
public final class Order {

    private final OrderId id;
    private final ProductId productId;
    private final CustomerId customerId;
    private final IdempotencyKey idempotencyKey;
    private final int quantity;
    private final Instant createdAt;
    private OrderStatus status;

    private Order(OrderId id, ProductId productId, CustomerId customerId,
                  IdempotencyKey idempotencyKey, int quantity, Instant createdAt,
                  OrderStatus status) {
        this.id = Objects.requireNonNull(id);
        this.productId = Objects.requireNonNull(productId);
        this.customerId = Objects.requireNonNull(customerId);
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.quantity = quantity;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.status = Objects.requireNonNull(status);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Order quantity must be positive");
        }
    }

    /**
     * Factory for a brand-new reservation: the moment stock was successfully
     * decremented in Redis. Always starts as RESERVED.
     */
    public static Order reserve(OrderId id, ProductId productId, CustomerId customerId,
                                IdempotencyKey idempotencyKey, int quantity, Instant createdAt) {
        return new Order(id, productId, customerId, idempotencyKey, quantity, createdAt, OrderStatus.RESERVED);
    }

    /**
     * Reconstitution factory used by persistence adapters (rehydrating from a DB row).
     * Bypasses no invariant — same validation applies.
     */
    public static Order restore(OrderId id, ProductId productId, CustomerId customerId,
                                IdempotencyKey idempotencyKey, int quantity, Instant createdAt,
                                OrderStatus status) {
        return new Order(id, productId, customerId, idempotencyKey, quantity, createdAt, status);
    }

    /**
     * Transition triggered by the batch worker after a successful Postgres insert.
     */
    public void confirm() {
        requireStatus(OrderStatus.RESERVED, "confirm");
        this.status = OrderStatus.CONFIRMED;
    }

    /**
     * Transition triggered by the batch worker after retries are exhausted.
     */
    public void fail() {
        requireStatus(OrderStatus.RESERVED, "fail");
        this.status = OrderStatus.FAILED;
    }

    /**
     * Transition triggered by the refund flow. Only a CONFIRMED order (one
     * that was actually persisted and fulfilled) can be refunded — refunding
     * a RESERVED or FAILED order makes no business sense since it was never
     * truly completed.
     */
    public void refund() {
        requireStatus(OrderStatus.CONFIRMED, "refund");
        this.status = OrderStatus.REFUNDED;
    }

    private void requireStatus(OrderStatus expected, String action) {
        if (this.status != expected) {
            throw new InvalidOrderStateTransitionException(
                    "Cannot %s order %s: expected status %s but was %s"
                            .formatted(action, id, expected, status));
        }
    }

    public OrderId id() {
        return id;
    }

    public ProductId productId() {
        return productId;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public IdempotencyKey idempotencyKey() {
        return idempotencyKey;
    }

    public int quantity() {
        return quantity;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public OrderStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order order)) return false;
        return id.equals(order.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
