package com.erichiroshi.blackfridaysaleflow.sale.application.usecase;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.GetOrderStatusUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.OrderStatusView;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.OrderPersistencePort;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderStatus;

public final class GetOrderStatusUseCaseImpl implements GetOrderStatusUseCase {

    private final OrderPersistencePort orderPersistencePort;

    public GetOrderStatusUseCaseImpl(OrderPersistencePort orderPersistencePort) {
        this.orderPersistencePort = orderPersistencePort;
    }

    @Override
    public OrderStatusView getStatus(OrderId orderId) {
        return orderPersistencePort.findById(orderId)
                .map(order -> new OrderStatusView(order.id(), order.status()))
                // Not found in Postgres yet is expected: the order may still
                // be sitting in the RabbitMQ batch, awaiting persistence.
                .orElse(new OrderStatusView(orderId, OrderStatus.RESERVED));
    }
}
