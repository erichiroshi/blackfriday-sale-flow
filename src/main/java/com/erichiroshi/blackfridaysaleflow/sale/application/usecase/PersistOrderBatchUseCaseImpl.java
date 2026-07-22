package com.erichiroshi.blackfridaysaleflow.sale.application.usecase;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.PersistOrderBatchUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.OrderPersistencePort;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.StockCachePort;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;

import java.util.List;

/**
 * Single responsibility: turn a batch of RESERVED orders into either
 * CONFIRMED (persisted) or FAILED+compensated. The retry/backoff mechanics
 * live in the RabbitMQ listener container configuration — by the time
 * {@link #compensateFailedBatch(List)} is called, retries are already
 * exhausted.
 */
public final class PersistOrderBatchUseCaseImpl implements PersistOrderBatchUseCase {

    private final OrderPersistencePort orderPersistencePort;
    private final StockCachePort stockCachePort;

    public PersistOrderBatchUseCaseImpl(OrderPersistencePort orderPersistencePort, StockCachePort stockCachePort) {
        this.orderPersistencePort = orderPersistencePort;
        this.stockCachePort = stockCachePort;
    }

    @Override
    public void persistBatch(List<Order> orders) {
        orders.forEach(Order::confirm);
        orderPersistencePort.saveAll(orders);
    }

    @Override
    public void compensateFailedBatch(List<Order> orders) {
        orders.forEach(order -> {
            order.fail();
            stockCachePort.release(order.productId());
        });
        orderPersistencePort.saveAll(orders);
    }
}
