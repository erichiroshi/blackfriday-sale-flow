package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.event;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.PersistOrderBatchUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;
import com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.messaging.OrderMessage;
import com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.messaging.OrderMessageMapper;
import com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.messaging.RabbitTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Input adapter: consumes orders in batches of 200 from RabbitMQ and drives
 * {@link PersistOrderBatchUseCase}.
 *
 * Retry policy (3 attempts, exponential backoff 1s/2s/4s) is applied per
 * batch via {@link RetryTemplate}. If all attempts fail, the whole batch is
 * compensated: every order is marked FAILED and its stock unit is released
 * back to Redis. This keeps the retry/backoff mechanics — an infrastructure
 * concern — entirely out of the domain and use case layers.
 */
@Component
public class OrderBatchConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderBatchConsumer.class);

    private final PersistOrderBatchUseCase persistOrderBatchUseCase;
    private final RetryTemplate batchRetryTemplate;

    public OrderBatchConsumer(PersistOrderBatchUseCase persistOrderBatchUseCase,
                               RetryTemplate batchRetryTemplate) {
        this.persistOrderBatchUseCase = persistOrderBatchUseCase;
        this.batchRetryTemplate = batchRetryTemplate;
    }

    @RabbitListener(queues = RabbitTopology.ORDERS_QUEUE, containerFactory = "batchRabbitListenerContainerFactory")
    public void consume(List<OrderMessage> messages) {
        List<Order> orders = messages.stream()
                .map(OrderMessageMapper::toDomain)
                .toList();

        log.info("Received batch of {} orders for persistence", orders.size());

        batchRetryTemplate.execute(
                context -> {
                    persistOrderBatchUseCase.persistBatch(orders);
                    log.info("Batch of {} orders persisted successfully (attempt {})",
                            orders.size(), context.getRetryCount() + 1);
                    return null;
                },
                context -> {
                    log.error("Batch of {} orders failed after all retries. Compensating stock and marking FAILED.",
                            orders.size(), context.getLastThrowable());
                    persistOrderBatchUseCase.compensateFailedBatch(orders);
                    return null;
                });
    }
}
