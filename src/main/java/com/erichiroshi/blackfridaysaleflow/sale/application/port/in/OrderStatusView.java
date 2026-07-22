package com.erichiroshi.blackfridaysaleflow.sale.application.port.in;

import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderStatus;

/**
 * Read model for order status polling. Deliberately NOT the same type as
 * {@link com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order}: a status query has a
 * valid answer (RESERVED) even when no row exists yet in Postgres, because
 * the order may still be in-flight in the RabbitMQ batch. That is expected
 * behavior in this architecture, not a 404.
 */
public record OrderStatusView(OrderId orderId, OrderStatus status) {
}
