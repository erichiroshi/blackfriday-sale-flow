package com.erichiroshi.blackfridaysaleflow.sale.application.usecase;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.RefundOrderUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.OrderPersistencePort;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.StockCachePort;
import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.RecordNotFoundException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;

/**
 * Refund flow: find the order, apply the domain's own refund() invariant
 * (only CONFIRMED -> REFUNDED is allowed — see {@link Order#refund()}),
 * persist the new status, then release the stock unit back to Redis.
 *
 * Order matters: we persist the REFUNDED status BEFORE releasing stock, so
 * if the release step fails, we never end up in a state where stock was
 * released but the order still (incorrectly) shows CONFIRMED — the reverse
 * ordering could let another shopper redeem the same physical unit twice.
 */
public final class RefundOrderUseCaseImpl implements RefundOrderUseCase {

    private final OrderPersistencePort orderPersistencePort;
    private final StockCachePort stockCachePort;

    public RefundOrderUseCaseImpl(OrderPersistencePort orderPersistencePort, StockCachePort stockCachePort) {
        this.orderPersistencePort = orderPersistencePort;
        this.stockCachePort = stockCachePort;
    }

    @Override
    public void refund(OrderId orderId) {
        Order order = orderPersistencePort.findById(orderId)
                .orElseThrow(() -> new RecordNotFoundException("Order %s not found".formatted(orderId)));

        order.refund();
        orderPersistencePort.update(order);
        stockCachePort.release(order.productId());
    }
}
