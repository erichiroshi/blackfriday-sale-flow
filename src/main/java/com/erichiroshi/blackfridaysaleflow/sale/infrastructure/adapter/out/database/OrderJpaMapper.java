package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.database;

import com.erichiroshi.blackfridaysaleflow.sale.domain.model.CustomerId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.IdempotencyKey;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderStatus;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;

final class OrderJpaMapper {

    private OrderJpaMapper() {
    }

    /**
     * Applies the domain order's current status onto an already-loaded JPA
     * entity, for the single-row update path (e.g. refund). Every other
     * field is immutable once the row exists, so only status is copied.
     */
    static OrderJpaEntity applyDomainStatus(OrderJpaEntity entity, Order order) {
        entity.setStatus(toJpaStatus(order.status()));
        return entity;
    }

    static Order toDomain(OrderJpaEntity entity) {
        return Order.restore(
                OrderId.of(entity.getId()),
                ProductId.of(entity.getProductId()),
                CustomerId.of(entity.getCustomerId()),
                IdempotencyKey.of(entity.getIdempotencyKey()),
                entity.getQuantity(),
                entity.getCreatedAt(),
                toDomainStatus(entity.getStatus()));
    }

    private static OrderStatusJpa toJpaStatus(OrderStatus status) {
        return OrderStatusJpa.valueOf(status.name());
    }

    private static OrderStatus toDomainStatus(OrderStatusJpa status) {
        return OrderStatus.valueOf(status.name());
    }
}
