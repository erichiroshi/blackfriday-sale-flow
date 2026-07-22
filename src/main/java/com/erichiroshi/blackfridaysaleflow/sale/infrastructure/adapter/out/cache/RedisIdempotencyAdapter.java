package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.cache;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.IdempotencyPort;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.IdempotencyKey;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed implementation of {@link IdempotencyPort}.
 *
 * Uses SET key value NX EX ttl (exposed by Spring as
 * {@code opsForValue().setIfAbsent(key, value, ttl)}), which is atomic on
 * its own — no Lua script needed here, unlike the stock counter.
 */
@Component
public class RedisIdempotencyAdapter implements IdempotencyPort {

    private static final String KEY_PREFIX = "idempotency:";
    // Long enough to cover the full async round trip (publish -> batch
    // consume -> persist) plus safety margin, short enough not to bloat Redis.
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public RedisIdempotencyAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<OrderId> putIfAbsent(IdempotencyKey key, OrderId candidateOrderId) {
        String redisKey = KEY_PREFIX + key.value();
        Boolean claimed = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, candidateOrderId.toString(), TTL);

        if (Boolean.TRUE.equals(claimed)) {
            return Optional.empty();
        }

        String existingValue = redisTemplate.opsForValue().get(redisKey);
        if (existingValue == null) {
            // Extremely unlikely race: the key expired between setIfAbsent
            // and the get. Treat as a fresh claim attempt failing safe by
            // retrying once.
            Boolean retryClaimed = redisTemplate.opsForValue()
                    .setIfAbsent(redisKey, candidateOrderId.toString(), TTL);
            if (Boolean.TRUE.equals(retryClaimed)) {
                return Optional.empty();
            }
            existingValue = redisTemplate.opsForValue().get(redisKey);
        }

        return Optional.of(OrderId.of(UUID.fromString(existingValue)));
    }
}
