package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.event;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.PersistOrderBatchUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.messaging.OrderMessage;
import io.github.resilience4j.springboot.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Proves that the declarative {@code @Retry(name = "orderBatchPersistence", ...)}
 * on {@link OrderBatchConsumer#consume(List)} really does retry 3 times
 * (per application.yml's resilience4j.retry.instances.orderBatchPersistence)
 * before invoking the fallback — the behavior {@link OrderBatchConsumerTest}
 * deliberately does not (and cannot, without a Spring proxy) verify.
 */
@SpringBootTest(classes = OrderBatchConsumerRetryIT.TestConfig.class)
@TestPropertySource(properties = {
        "resilience4j.retry.instances.orderBatchPersistence.max-attempts=3",
        "resilience4j.retry.instances.orderBatchPersistence.wait-duration=10ms",
        "resilience4j.retry.instances.orderBatchPersistence.enable-exponential-backoff=false"
})
class OrderBatchConsumerRetryIT {

    @Autowired
    private OrderBatchConsumer orderBatchConsumer;

    @Autowired
    private PersistOrderBatchUseCase persistOrderBatchUseCase;

    private OrderMessage sampleMessage() {
        return new OrderMessage("11111111-1111-1111-1111-111111111111", "TV",
                "CUSTOMER-1", "idem-1", 1, Instant.now());
    }

    @Test
    void retriesThreeTimesThenCompensates() {
        doThrow(new RuntimeException("Postgres unavailable"))
                .when(persistOrderBatchUseCase).persistBatch(anyList());

        orderBatchConsumer.consume(List.of(sampleMessage()));

        verify(persistOrderBatchUseCase, times(3)).persistBatch(anyList());
        verify(persistOrderBatchUseCase, times(1)).compensateFailedBatch(anyList());
    }

    @Configuration
    @Import({AopAutoConfiguration.class, RetryAutoConfiguration.class})
    static class TestConfig {

        @Bean
        PersistOrderBatchUseCase persistOrderBatchUseCase() {
            return mock(PersistOrderBatchUseCase.class);
        }

        @Bean
        OrderBatchConsumer orderBatchConsumer(PersistOrderBatchUseCase persistOrderBatchUseCase) {
            return new OrderBatchConsumer(persistOrderBatchUseCase);
        }
    }
}
