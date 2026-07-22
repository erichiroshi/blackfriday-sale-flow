package com.erichiroshi.blackfridaysaleflow.sale.application.port.out;

import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;

/**
 * Driven port for publishing a freshly reserved order onto the async
 * pipeline (RabbitMQ). The use case depends only on this abstraction —
 * nothing here mentions AMQP, exchanges, or routing keys.
 */
public interface OrderQueuePort {

    void publish(Order order);
}
