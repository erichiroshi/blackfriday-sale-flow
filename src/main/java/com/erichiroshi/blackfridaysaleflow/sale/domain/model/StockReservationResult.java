package com.erichiroshi.blackfridaysaleflow.sale.domain.model;

/**
 * Outcome of an atomic stock reservation attempt against the Redis counter.
 *
 * remainingStock is only meaningful when successful == true; it reflects the
 * counter value immediately after the decrement (may be used for observability
 * / "only N left" messaging).
 */
public record StockReservationResult(boolean successful, long remainingStock) {

    public static StockReservationResult success(long remainingStock) {
        return new StockReservationResult(true, remainingStock);
    }

    public static StockReservationResult outOfStock() {
        return new StockReservationResult(false, 0);
    }
}
