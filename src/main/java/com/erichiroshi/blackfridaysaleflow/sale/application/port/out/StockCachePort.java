package com.erichiroshi.blackfridaysaleflow.sale.application.port.out;

import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.StockReservationResult;

/**
 * Driven port for the Redis-backed stock counter.
 *
 * Implementations MUST guarantee atomicity: a decrement that would push the
 * counter below zero must be compensated (incremented back) in the same
 * atomic operation, never as two separate round-trips. This is what keeps
 * 10k concurrent requests against 100 units consistent.
 */
public interface StockCachePort {

    /**
     * Atomically attempts to reserve one unit of stock for the given product.
     *
     * @return a successful result with the remaining stock count, or an
     *         unsuccessful result if no stock was available.
     */
    StockReservationResult reserve(ProductId productId);

    /**
     * Compensates a previously successful reservation (increments the
     * counter back). Used by the batch worker when persistence permanently
     * fails after exhausting retries, and by the refund flow.
     */
    void release(ProductId productId);

    /**
     * Creates the stock counter for a product, only if it does not already
     * exist (SETNX semantics) — restarting the app or re-running the seed
     * never resets stock mid-sale.
     *
     * @return true if this call created the counter, false if it already existed.
     */
    boolean initialize(ProductId productId, long initialStock);

    /**
     * Atomically adds units to an existing counter (e.g. an admin
     * replenishing stock while the sale is live).
     *
     * @return the new total after the increment.
     * @throws IllegalStateException if the product was never initialized.
     */
    long replenish(ProductId productId, long additionalUnits);
}
