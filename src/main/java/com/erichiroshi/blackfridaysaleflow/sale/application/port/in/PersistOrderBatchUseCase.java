package com.erichiroshi.blackfridaysaleflow.sale.application.port.in;

import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;

import java.util.List;

/**
 * Driver port for the async worker: attempts to persist a batch of reserved
 * orders in Postgres. On success, orders become CONFIRMED. On unrecoverable
 * failure (retries exhausted), orders become FAILED and their stock is
 * released back to Redis.
 *
 * The retry loop itself lives in the RabbitMQ adapter configuration
 * (listener container retry policy), not in this use case: this use case
 * represents a single attempt and reports back whether it succeeded so the
 * adapter can decide whether to retry, DLQ, or trigger compensation.
 */
public interface PersistOrderBatchUseCase {

    void persistBatch(List<Order> orders);

    /**
     * Called by the adapter once retries are exhausted for a batch: marks
     * every order FAILED and compensates (releases) their reserved stock.
     */
    void compensateFailedBatch(List<Order> orders);
}
