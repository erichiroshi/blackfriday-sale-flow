package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.database;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Database Object (DBO) for the orders table. Kept entirely separate from
 * the domain {@link com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order} — no JPA
 * annotation ever touches the domain layer.
 */
@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"))
public class OrderJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatusJpa status;

    protected OrderJpaEntity() {
        // required by JPA
    }

    public OrderJpaEntity(UUID id, String productId, String customerId, String idempotencyKey,
                          int quantity, Instant createdAt, OrderStatusJpa status) {
        this.id = id;
        this.productId = productId;
        this.customerId = customerId;
        this.idempotencyKey = idempotencyKey;
        this.quantity = quantity;
        this.createdAt = createdAt;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public int getQuantity() {
        return quantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public OrderStatusJpa getStatus() {
        return status;
    }

    /**
     * Package-private on purpose: only {@link OrderJpaMapper} (via
     * {@link OrderPersistenceAdapter#update}) mutates status after the row
     * already exists. Nothing else in this adapter package needs write access.
     */
    void setStatus(OrderStatusJpa status) {
        this.status = status;
    }
}
