package com.erichiroshi.blackfridaysaleflow.sale.application.port.in;

import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.OutOfStockException;

/**
 * Driver port: reserve one unit of stock, synchronously, in milliseconds.
 * Throws {@link OutOfStockException} when the product has no stock left.
 */
public interface ReserveStockUseCase {

    ReservationResponse reserve(ReserveStockCommand command);
}
