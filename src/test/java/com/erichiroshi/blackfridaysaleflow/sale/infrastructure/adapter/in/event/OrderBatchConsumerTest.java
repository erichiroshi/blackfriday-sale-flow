package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.event;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.PersistOrderBatchUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.messaging.OrderMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderBatchConsumerTest {

    @Mock
    private PersistOrderBatchUseCase persistOrderBatchUseCase;

    private RetryTemplate fastRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
        // No real backoff here — keeps the test fast; the backoff policy
        // itself is exercised in RabbitConfig wiring, not in this test.
        return retryTemplate;
    }

    private OrderMessage sampleMessage() {
        return new OrderMessage("11111111-1111-1111-1111-111111111111", "PRODUCT-1",
                "CUSTOMER-1", "idem-1", 1, Instant.now());
    }

    @Test
    void persistsBatchSuccessfullyOnFirstAttempt() {
        OrderBatchConsumer consumer = new OrderBatchConsumer(persistOrderBatchUseCase, fastRetryTemplate());

        consumer.consume(List.of(sampleMessage()));

        verify(persistOrderBatchUseCase, times(1)).persistBatch(anyList());
        verify(persistOrderBatchUseCase, times(0)).compensateFailedBatch(anyList());
    }

    @Test
    void compensatesBatchAfterAllRetriesFail() {
        OrderBatchConsumer consumer = new OrderBatchConsumer(persistOrderBatchUseCase, fastRetryTemplate());
        doThrow(new RuntimeException("Postgres unavailable"))
                .when(persistOrderBatchUseCase).persistBatch(anyList());

        consumer.consume(List.of(sampleMessage()));

        verify(persistOrderBatchUseCase, times(3)).persistBatch(anyList());
        verify(persistOrderBatchUseCase, times(1)).compensateFailedBatch(anyList());
    }
}
