package com.erichiroshi.blackfridaysaleflow.sale.application.usecase;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ReservationResponse;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ReserveStockCommand;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.IdempotencyPort;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.OrderPersistencePort;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.OrderQueuePort;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.StockCachePort;
import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.OutOfStockException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.CustomerId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.IdempotencyKey;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderStatus;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.StockReservationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReserveStockUseCaseImplTest {

    @Mock
    private StockCachePort stockCachePort;
    @Mock
    private IdempotencyPort idempotencyPort;
    @Mock
    private OrderQueuePort orderQueuePort;
    @Mock
    private OrderPersistencePort orderPersistencePort;

    private ReserveStockUseCaseImpl useCase;
    private ReserveStockCommand command;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-27T00:00:00Z"), ZoneOffset.UTC);
        useCase = new ReserveStockUseCaseImpl(stockCachePort, idempotencyPort, orderQueuePort, orderPersistencePort, fixedClock);
        command = new ReserveStockCommand(
                ProductId.of("PRODUCT-1"),
                CustomerId.of("CUSTOMER-1"),
                IdempotencyKey.of("idem-key-1"));
    }

    @Test
    void reservesStockAndPublishesWhenAvailable() {
        when(idempotencyPort.putIfAbsent(eq(command.idempotencyKey()), any())).thenReturn(Optional.empty());
        when(stockCachePort.reserve(command.productId())).thenReturn(StockReservationResult.success(99));

        ReservationResponse response = useCase.reserve(command);

        assertThat(response.status()).isEqualTo(OrderStatus.RESERVED);
        assertThat(response.idempotentReplay()).isFalse();
        verify(orderQueuePort).publish(any(Order.class));
    }

    @Test
    void throwsOutOfStockAndNeverPublishesWhenRedisSaysNoStock() {
        when(idempotencyPort.putIfAbsent(eq(command.idempotencyKey()), any())).thenReturn(Optional.empty());
        when(stockCachePort.reserve(command.productId())).thenReturn(StockReservationResult.outOfStock());

        assertThatThrownBy(() -> useCase.reserve(command))
                .isInstanceOf(OutOfStockException.class);

        verify(orderQueuePort, never()).publish(any());
    }

    @Test
    void replaysExistingOrderWithoutTouchingStockWhenIdempotencyKeySeenBefore() {
        OrderId existingOrderId = OrderId.newId();
        when(idempotencyPort.putIfAbsent(eq(command.idempotencyKey()), any())).thenReturn(Optional.of(existingOrderId));
        when(orderPersistencePort.findById(existingOrderId)).thenReturn(Optional.empty());

        ReservationResponse response = useCase.reserve(command);

        assertThat(response.idempotentReplay()).isTrue();
        assertThat(response.orderId()).isEqualTo(existingOrderId);
        // Still in-flight (not yet persisted by the worker) -> RESERVED, not an error.
        assertThat(response.status()).isEqualTo(OrderStatus.RESERVED);
        verify(stockCachePort, never()).reserve(any());
        verify(orderQueuePort, never()).publish(any());
    }

    @Test
    void replaysExistingOrderReflectingItsPersistedStatus() {
        OrderId existingOrderId = OrderId.newId();
        Order persistedOrder = Order.restore(
                existingOrderId, command.productId(), command.customerId(), command.idempotencyKey(),
                1, Instant.now(), OrderStatus.CONFIRMED);

        when(idempotencyPort.putIfAbsent(eq(command.idempotencyKey()), any())).thenReturn(Optional.of(existingOrderId));
        when(orderPersistencePort.findById(existingOrderId)).thenReturn(Optional.of(persistedOrder));

        ReservationResponse response = useCase.reserve(command);

        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.idempotentReplay()).isTrue();
    }
}
