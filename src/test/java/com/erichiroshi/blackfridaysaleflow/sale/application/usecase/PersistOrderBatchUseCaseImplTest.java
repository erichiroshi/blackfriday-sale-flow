package com.erichiroshi.blackfridaysaleflow.sale.application.usecase;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.OrderPersistencePort;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.StockCachePort;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.CustomerId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.IdempotencyKey;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderStatus;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PersistOrderBatchUseCaseImplTest {

    @Mock
    private OrderPersistencePort orderPersistencePort;
    @Mock
    private StockCachePort stockCachePort;

    @Captor
    private ArgumentCaptor<List<Order>> orderListCaptor;

    @InjectMocks
    private PersistOrderBatchUseCaseImpl useCase;

    private Order newReservedOrder(ProductId productId) {
        return Order.reserve(OrderId.newId(), productId, CustomerId.of("CUSTOMER-1"),
                IdempotencyKey.of("idem-" + System.nanoTime()), 1, Instant.now());
    }

    @Test
    void persistBatchConfirmsEveryOrderAndSavesTheWholeBatch() {
        List<Order> orders = List.of(newReservedOrder(ProductId.of("PRODUCT-1")), newReservedOrder(ProductId.of("PRODUCT-1")));

        useCase.persistBatch(orders);

        assertThat(orders).allMatch(order -> order.status() == OrderStatus.CONFIRMED);
        verify(orderPersistencePort).saveAll(orders);
    }

    @Test
    void compensateFailedBatchMarksEveryOrderFailedAndReleasesItsStock() {
        ProductId productId = ProductId.of("PRODUCT-1");
        List<Order> orders = List.of(newReservedOrder(productId), newReservedOrder(productId));

        useCase.compensateFailedBatch(orders);

        assertThat(orders).allMatch(order -> order.status() == OrderStatus.FAILED);
        verify(stockCachePort, times(orders.size())).release(productId);

        // Utilize o captor injetado pela anotação
        verify(orderPersistencePort).saveAll(orderListCaptor.capture());
        assertThat(orderListCaptor.getValue()).hasSize(2);
    }
}
