package com.erichiroshi.blackfridaysaleflow.sale.application.usecase;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.OrderStatusView;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.OrderPersistencePort;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.CustomerId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.IdempotencyKey;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderStatus;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrderStatusUseCaseImplTest {

    @Mock
    private OrderPersistencePort orderPersistencePort;

    @Test
    void returnsPersistedStatusWhenOrderExists() {
        GetOrderStatusUseCaseImpl useCase = new GetOrderStatusUseCaseImpl(orderPersistencePort);
        OrderId orderId = OrderId.newId();
        Order order = Order.restore(orderId, ProductId.of("PRODUCT-1"), CustomerId.of("CUSTOMER-1"),
                IdempotencyKey.of("idem-1"), 1, Instant.now(), OrderStatus.CONFIRMED);
        when(orderPersistencePort.findById(orderId)).thenReturn(Optional.of(order));

        OrderStatusView view = useCase.getStatus(orderId);

        assertThat(view.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(view.orderId()).isEqualTo(orderId);
    }

    @Test
    void returnsReservedWhenOrderNotYetPersisted() {
        GetOrderStatusUseCaseImpl useCase = new GetOrderStatusUseCaseImpl(orderPersistencePort);
        OrderId orderId = OrderId.newId();
        when(orderPersistencePort.findById(orderId)).thenReturn(Optional.empty());

        OrderStatusView view = useCase.getStatus(orderId);

        assertThat(view.status()).isEqualTo(OrderStatus.RESERVED);
    }
}
