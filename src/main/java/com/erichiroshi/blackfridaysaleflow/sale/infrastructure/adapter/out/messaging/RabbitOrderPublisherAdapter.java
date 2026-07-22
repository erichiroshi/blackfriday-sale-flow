package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.messaging;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.OrderQueuePort;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitOrderPublisherAdapter implements OrderQueuePort {

    private static final Logger log = LoggerFactory.getLogger(RabbitOrderPublisherAdapter.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitOrderPublisherAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(Order order) {
        OrderMessage message = OrderMessageMapper.toMessage(order);
        rabbitTemplate.convertAndSend(
                RabbitTopology.ORDERS_EXCHANGE,
                RabbitTopology.ORDERS_ROUTING_KEY,
                message);
        log.debug("Published order {} to {}", order.id(), RabbitTopology.ORDERS_QUEUE);
    }
}
