package com.erichiroshi.blackfridaysaleflow.sale.application.port.in;

import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderStatus;

/**
 * Result handed back to the driving adapter after a reservation attempt.
 * {@code idempotentReplay} tells the web adapter it should still answer
 * 202 (not a new reservation, but not an error either).
 */
public record ReservationResponse(OrderId orderId, OrderStatus status, boolean idempotentReplay) {

    public static ReservationResponse newReservation(OrderId orderId) {
        return new ReservationResponse(orderId, OrderStatus.RESERVED, false);
    }

    public static ReservationResponse replay(OrderId existingOrderId, OrderStatus currentStatus) {
        return new ReservationResponse(existingOrderId, currentStatus, true);
    }
}
