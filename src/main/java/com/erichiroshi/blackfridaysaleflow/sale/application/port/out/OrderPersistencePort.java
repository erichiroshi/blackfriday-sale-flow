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
     * Persists (insert-or-update) the given batch in a single transaction.
     * Must be all-or-nothing: a partial failure rolls back the whole batch,
     * so the caller can safely retry.
     */
    void saveAll(List<Order> orders);

    Optional<Order> findById(OrderId orderId);
}
