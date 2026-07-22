package com.erichiroshi.blackfridaysaleflow.sale.application.port.in;

import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.InvalidOrderStateTransitionException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.RecordNotFoundException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;

/**
 * Driver port for the refund flow: only a CONFIRMED order can be refunded.
 * On success the order becomes REFUNDED and its stock unit is released back
 * to Redis for resale.
 */
public interface RefundOrderUseCase {

    void refund(OrderId orderId) throws RecordNotFoundException, InvalidOrderStateTransitionException;
}
