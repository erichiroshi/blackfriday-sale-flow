package com.erichiroshi.blackfridaysaleflow.sale.application.usecase;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.OrderPersistencePort;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.StockCachePort;
import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.InvalidOrderStateTransitionException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.RecordNotFoundException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.CustomerId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.IdempotencyKey;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderStatus;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundOrderUseCaseImplTest {

    @Mock
    private OrderPersistencePort orderPersistencePort;
    @Mock
    private StockCachePort stockCachePort;

    @InjectMocks
    private RefundOrderUseCaseImpl useCase;

    private Order confirmedOrder(OrderId orderId, ProductId productId) {
        return Order.restore(orderId, productId, CustomerId.of("CUSTOMER-1"), IdempotencyKey.of("idem-1"),
                1, Instant.now(), OrderStatus.CONFIRMED);
    }

    @Test
    void refundsConfirmedOrderAndReleasesStock() {
        OrderId orderId = OrderId.newId();
        ProductId productId = ProductId.of("TV");
        Order order = confirmedOrder(orderId, productId);
        when(orderPersistencePort.findById(orderId)).thenReturn(Optional.of(order));

        useCase.refund(orderId);

        assertThat(order.status()).isEqualTo(OrderStatus.REFUNDED);
        verify(orderPersistencePort).update(order);
        verify(stockCachePort).release(productId);
    }

    @Test
    void throwsNotFoundWhenOrderDoesNotExist() {
        OrderId orderId = OrderId.newId();
        when(orderPersistencePort.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.refund(orderId)).isInstanceOf(RecordNotFoundException.class);

        verify(stockCachePort, never()).release(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void throwsInvalidTransitionWhenOrderIsNotConfirmed() {
        OrderId orderId = OrderId.newId();
        Order reservedOrder = Order.reserve(orderId, ProductId.of("PC"), CustomerId.of("CUSTOMER-1"),
                IdempotencyKey.of("idem-2"), 1, Instant.now());
        when(orderPersistencePort.findById(orderId)).thenReturn(Optional.of(reservedOrder));

        assertThatThrownBy(() -> useCase.refund(orderId)).isInstanceOf(InvalidOrderStateTransitionException.class);

        verify(orderPersistencePort, never()).update(org.mockito.ArgumentMatchers.any());
        verify(stockCachePort, never()).release(org.mockito.ArgumentMatchers.any());
    }
}
