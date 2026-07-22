package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.messaging;

/**
 * Central place for exchange/queue/routing-key names, so the config class
 * and the adapters never hardcode strings independently.
 */
public final class RabbitTopology {

    public static final String ORDERS_EXCHANGE = "orders.exchange";
    public static final String ORDERS_QUEUE = "orders.reservation.queue";
    public static final String ORDERS_ROUTING_KEY = "orders.reservation";

    public static final String ORDERS_DLX = "orders.dlx";
    public static final String ORDERS_DLQ = "orders.reservation.dlq";
    public static final String ORDERS_DLQ_ROUTING_KEY = "orders.reservation.dlq";

    private RabbitTopology() {
    }
}
