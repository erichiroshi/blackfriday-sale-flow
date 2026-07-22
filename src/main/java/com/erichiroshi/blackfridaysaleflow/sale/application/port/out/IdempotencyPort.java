package com.erichiroshi.blackfridaysaleflow.sale.application.port.out;

import com.erichiroshi.blackfridaysaleflow.sale.domain.model.IdempotencyKey;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;

import java.util.Optional;

/**
 * Driven port guaranteeing that a retried request carrying the same
 * Idempotency-Key never results in a second stock decrement.
 */
public interface IdempotencyPort {

    /**
     * Atomically checks whether {@code key} is already associated with an
     * order. If not, associates it with {@code candidateOrderId} in the same
     * operation (e.g. Redis SETNX).
     *
     * @return the pre-existing OrderId if this key was already seen, or
     *         empty if {@code candidateOrderId} was just claimed as the
     *         owner of this key.
     */
    Optional<OrderId> putIfAbsent(IdempotencyKey key, OrderId candidateOrderId);
}
