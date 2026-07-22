package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.event;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.PersistOrderBatchUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.messaging.OrderMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * These tests exercise {@link OrderBatchConsumer}'s own logic directly —
 * message-to-domain conversion, delegating to the use case, and the
 * fallback's compensation call. The retry-loop mechanics themselves
 * (3 attempts, exponential backoff) are Resilience4j's responsibility,
 * already covered by that library's own test suite, and are wired via the
 * {@code @Retry} annotation + {@code resilience4j.retry.instances.orderBatchPersistence.*}
 * in application.yml (see {@code RabbitConfig} javadoc).
 */
@ExtendWith(MockitoExtension.class)
class OrderBatchConsumerTest {

    @Mock
    private PersistOrderBatchUseCase persistOrderBatchUseCase;

    @InjectMocks
    private OrderBatchConsumer consumer;

    private OrderMessage sampleMessage() {
        return new OrderMessage("11111111-1111-1111-1111-111111111111", "TV",
                "CUSTOMER-1", "idem-1", 1, Instant.now());
    }

    @Test
    void consumeDelegatesToPersistBatch() {
        consumer.consume(List.of(sampleMessage()));

        verify(persistOrderBatchUseCase, times(1)).persistBatch(anyList());
    }

    @Test
    void fallbackConsumeDelegatesToCompensateFailedBatch() {
        consumer.fallbackConsume(List.of(sampleMessage()), new RuntimeException("Postgres unavailable"));

        verify(persistOrderBatchUseCase, times(1)).compensateFailedBatch(anyList());
        verify(persistOrderBatchUseCase, times(0)).persistBatch(anyList());
    }
}
