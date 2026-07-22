package com.erichiroshi.blackfridaysaleflow.sale.application.port.out;

import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;

import java.util.List;
import java.util.Optional;

/**
 * Driven port for persisting orders. saveAll is expected to be a single
 * batched transaction — the whole point of consuming in batches of 200 is to
 * avoid 200 separate round-trips to Postgres.
 */
public interface OrderPersistencePort {

    /**
     * Batch-inserts new orders in a single transaction. Must be all-or-nothing:
     * a partial failure rolls back the whole batch, so the caller can safely
     * retry. Every order here is being persisted for the first time — use
     * {@link #update(Order)} for changes to an already-persisted order.
     */
    void saveAll(List<Order> orders);

    /**
     * Updates a single, already-persisted order (e.g. a CONFIRMED -> REFUNDED
     * transition). Distinct from {@link #saveAll(List)}, which is the
     * high-throughput batch-insert path used by the worker.
     */
    void update(Order order);

    Optional<Order> findById(OrderId orderId);
}
