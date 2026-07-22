package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.event;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.PersistOrderBatchUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;
import com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.messaging.OrderMessage;
import com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.messaging.OrderMessageMapper;
import com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.messaging.RabbitTopology;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Input adapter: consumes orders in batches of 200 from RabbitMQ and drives
 * {@link PersistOrderBatchUseCase}.
 *
 * Retry policy (3 attempts, exponential backoff 1s/2s/4s — see
 * application.yml under resilience4j.retry.instances.orderBatchPersistence)
 * is applied declaratively via {@code @Retry}. This works because the
 * RabbitMQ container invokes {@code consume(...)} on the Spring-managed
 * proxy of this bean, so the AOP interceptor backing the annotation is in
 * the call path — no self-invocation pitfall here.
 *
 * If all attempts fail, {@link #fallbackConsume(List, Throwable)} runs
 * instead: the whole batch is compensated (marked FAILED, stock released).
 */
@Component
public class OrderBatchConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderBatchConsumer.class);

    private final PersistOrderBatchUseCase persistOrderBatchUseCase;

    public OrderBatchConsumer(PersistOrderBatchUseCase persistOrderBatchUseCase) {
        this.persistOrderBatchUseCase = persistOrderBatchUseCase;
    }

    @Retry(name = "orderBatchPersistence", fallbackMethod = "fallbackConsume")
    @RabbitListener(queues = RabbitTopology.ORDERS_QUEUE, containerFactory = "batchRabbitListenerContainerFactory")
    public void consume(List<OrderMessage> messages) {
        List<Order> orders = toOrders(messages);
        log.info("Received batch of {} orders for persistence", orders.size());
        persistOrderBatchUseCase.persistBatch(orders);
        log.info("Batch of {} orders persisted successfully", orders.size());
    }

    /**
     * Invoked by Resilience4j once every retry attempt has failed. Signature
     * must mirror {@link #consume(List)} plus the trailing {@link Throwable}.
     */
    void fallbackConsume(List<OrderMessage> messages, Throwable throwable) {
        List<Order> orders = toOrders(messages);
        log.error("Batch of {} orders failed after all retry attempts. Compensating stock and marking FAILED.",
                orders.size(), throwable);
        persistOrderBatchUseCase.compensateFailedBatch(orders);
    }

    private List<Order> toOrders(List<OrderMessage> messages) {
        return messages.stream()
                .map(OrderMessageMapper::toDomain)
                .toList();
    }
}
