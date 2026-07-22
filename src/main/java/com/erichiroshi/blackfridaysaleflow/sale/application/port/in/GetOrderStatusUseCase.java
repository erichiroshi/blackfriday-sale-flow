package com.erichiroshi.blackfridaysaleflow.sale.application.port.in;

import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;

/**
 * Driver port: lets the client poll for the outcome of a reservation.
 * Never throws for an order that simply has not been persisted yet — see
 * {@link OrderStatusView}.
 */
public interface GetOrderStatusUseCase {

    OrderStatusView getStatus(OrderId orderId);
}
