package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.messaging;

import java.time.Instant;

/**
 * Wire format published to RabbitMQ. Deliberately a separate type from the
 * domain {@link com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order} — the queue payload
 * is a serialization contract, not a business object, and must stay
 * decoupled so the domain model can evolve without breaking consumers.
 */
public record OrderMessage(
        String orderId,
        String productId,
        String customerId,
        String idempotencyKey,
        int quantity,
        Instant createdAt) {
}
