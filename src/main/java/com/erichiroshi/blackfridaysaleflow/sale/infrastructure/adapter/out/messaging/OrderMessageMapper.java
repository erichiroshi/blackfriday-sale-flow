package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.messaging;

import com.erichiroshi.blackfridaysaleflow.sale.domain.model.CustomerId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.IdempotencyKey;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;

public final class OrderMessageMapper {

    private OrderMessageMapper() {
    }

    public static OrderMessage toMessage(Order order) {
        return new OrderMessage(
                order.id().toString(),
                order.productId().value(),
                order.customerId().value(),
                order.idempotencyKey().value(),
                order.quantity(),
                order.createdAt());
    }

    public static Order toDomain(OrderMessage message) {
        return Order.reserve(
                OrderId.of(message.orderId()),
                ProductId.of(message.productId()),
                CustomerId.of(message.customerId()),
                IdempotencyKey.of(message.idempotencyKey()),
                message.quantity(),
                message.createdAt());
    }
}
