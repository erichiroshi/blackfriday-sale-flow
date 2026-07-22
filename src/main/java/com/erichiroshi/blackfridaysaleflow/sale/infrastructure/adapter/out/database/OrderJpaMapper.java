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

    static OrderJpaEntity toEntity(Order order) {
        return new OrderJpaEntity(
                order.id().value(),
                order.productId().value(),
                order.customerId().value(),
                order.idempotencyKey().value(),
                order.quantity(),
                order.createdAt(),
                toJpaStatus(order.status()));
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
