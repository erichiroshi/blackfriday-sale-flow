package com.erichiroshi.blackfridaysaleflow.sale.application.usecase;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ReservationResponse;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ReserveStockCommand;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ReserveStockUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.IdempotencyPort;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.OrderPersistencePort;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.OrderQueuePort;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.StockCachePort;
import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.OutOfStockException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderStatus;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.StockReservationResult;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Synchronous flow: validate idempotency, atomically reserve stock in Redis,
 * publish to RabbitMQ for the async worker to persist later. Never touches
 * Postgres directly — that is the whole point of the architecture.
 *
 * Single responsibility: this use case does exactly one business action —
 * reserve stock for one order. Nothing else.
 */
public final class ReserveStockUseCaseImpl implements ReserveStockUseCase {

    private static final int DEFAULT_QUANTITY = 1;

    private final StockCachePort stockCachePort;
    private final IdempotencyPort idempotencyPort;
    private final OrderQueuePort orderQueuePort;
    private final OrderPersistencePort orderPersistencePort;
    private final Clock clock;

    public ReserveStockUseCaseImpl(StockCachePort stockCachePort,
                                    IdempotencyPort idempotencyPort,
                                    OrderQueuePort orderQueuePort,
                                    OrderPersistencePort orderPersistencePort,
                                    Clock clock) {
        this.stockCachePort = stockCachePort;
        this.idempotencyPort = idempotencyPort;
        this.orderQueuePort = orderQueuePort;
        this.orderPersistencePort = orderPersistencePort;
        this.clock = clock;
    }

    @Override
    public ReservationResponse reserve(ReserveStockCommand command) {
        OrderId candidateOrderId = OrderId.newId();

        Optional<OrderId> existingOrderId = idempotencyPort.putIfAbsent(command.idempotencyKey(), candidateOrderId);
        if (existingOrderId.isPresent()) {
            return replayExisting(existingOrderId.get());
        }

        StockReservationResult reservationResult = stockCachePort.reserve(command.productId());
        if (!reservationResult.successful()) {
            throw new OutOfStockException(command.productId());
        }

        Order order = Order.reserve(
                candidateOrderId,
                command.productId(),
                command.customerId(),
                command.idempotencyKey(),
                DEFAULT_QUANTITY,
                Instant.now(clock));

        orderQueuePort.publish(order);

        return ReservationResponse.newReservation(candidateOrderId);
    }

    private ReservationResponse replayExisting(OrderId existingOrderId) {
        Order existingOrder = orderPersistencePort.findById(existingOrderId)
                .orElse(null);
        // The order may still be in-flight (published but not yet persisted
        // by the batch worker), so "not found in Postgres yet" is not an
        // error here — it just means it is still RESERVED.
        if (existingOrder == null) {
            return ReservationResponse.replay(existingOrderId, OrderStatus.RESERVED);
        }
        return ReservationResponse.replay(existingOrderId, existingOrder.status());
    }
}
