package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.config;

import com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.messaging.RabbitTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology and batch-consumption wiring.
 *
 * Topology: a direct exchange -> main queue, with a dead-letter exchange/queue
 * attached for operational visibility (messages whose batch permanently
 * fails are still routed there after this app's own compensation logic
 * runs — see {@link com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.event.OrderBatchConsumer}).
 *
 * Batch consumption: the container factory is configured for batches of 200
 * with a timeout, so the worker never waits forever for a straggling 200th
 * message during low-traffic periods.
 *
 * Retry/backoff for a persistence attempt is NOT configured here — it is
 * declarative, via {@code @Retry} on {@code OrderBatchConsumer.consume(...)},
 * backed by the {@code resilience4j.retry.instances.orderBatchPersistence.*}
 * properties in application.yml.
 */
@Configuration
public class RabbitConfig {

    private static final int BATCH_SIZE = 200;
    private static final int BATCH_RECEIVE_TIMEOUT_MS = 5_000;

    @Bean
    public DirectExchange ordersExchange() {
        return new DirectExchange(RabbitTopology.ORDERS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange ordersDeadLetterExchange() {
        return new DirectExchange(RabbitTopology.ORDERS_DLX, true, false);
    }

    @Bean
    public Queue ordersQueue() {
        return QueueBuilder.durable(RabbitTopology.ORDERS_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitTopology.ORDERS_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitTopology.ORDERS_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue ordersDeadLetterQueue() {
        return QueueBuilder.durable(RabbitTopology.ORDERS_DLQ).build();
    }

    @Bean
    public Binding ordersBinding() {
        return BindingBuilder.bind(ordersQueue())
                .to(ordersExchange())
                .with(RabbitTopology.ORDERS_ROUTING_KEY);
    }

    @Bean
    public Binding ordersDeadLetterBinding() {
        return BindingBuilder.bind(ordersDeadLetterQueue())
                .to(ordersDeadLetterExchange())
                .with(RabbitTopology.ORDERS_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    /**
     * Batch listener container factory: groups up to 200 messages (or
     * whatever accumulates within the timeout) into a single
     * {@code List<OrderMessage>} delivered to the consumer method.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory batchRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);

        factory.setBatchListener(true);
        factory.setConsumerBatchEnabled(true);
        factory.setBatchSize(BATCH_SIZE);
        factory.setReceiveTimeout((long) BATCH_RECEIVE_TIMEOUT_MS);
        return factory;
    }
}
