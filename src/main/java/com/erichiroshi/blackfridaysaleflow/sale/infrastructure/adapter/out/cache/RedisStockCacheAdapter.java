package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.cache;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.StockCachePort;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.StockReservationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis-backed implementation of {@link StockCachePort}.
 *
 * The reservation itself runs as a single Lua script (EVAL) so the
 * check-decrement-compensate sequence is atomic from Redis' point of view,
 * regardless of how many clients call concurrently.
 */
@Component
public class RedisStockCacheAdapter implements StockCachePort {

    private static final Logger log = LoggerFactory.getLogger(RedisStockCacheAdapter.class);
    private static final String KEY_PREFIX = "stock:";
    private static final long OUT_OF_STOCK = -1L;
    private static final long KEY_NOT_FOUND = -2L;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> reserveStockScript;

    public RedisStockCacheAdapter(StringRedisTemplate redisTemplate,
                                  DefaultRedisScript<Long> reserveStockScript) {
        this.redisTemplate = redisTemplate;
        this.reserveStockScript = reserveStockScript;
    }

    @Override
    public StockReservationResult reserve(ProductId productId) {
        String key = stockKey(productId);
        Long result = redisTemplate.execute(reserveStockScript, List.of(key));

        if (result == null) {
            log.error("Redis reservation script returned null for key {}", key);
            throw new IllegalStateException("Unexpected null response from Redis reservation script");
        }
        if (result == KEY_NOT_FOUND) {
            throw new IllegalStateException(
                    "Stock key %s was never initialized in Redis".formatted(key));
        }
        if (result == OUT_OF_STOCK) {
            log.info("Product {} is out of stock", productId.value());
            return StockReservationResult.outOfStock();
        }
        return StockReservationResult.success(result);
    }

    @Override
    public void release(ProductId productId) {
        String key = stockKey(productId);
        Long newValue = redisTemplate.opsForValue().increment(key);
        log.info("Released 1 unit of stock for product {} (new counter value: {})", productId.value(), newValue);
    }

    @Override
    public boolean initialize(ProductId productId, long initialStock) {
        String key = stockKey(productId);
        Boolean created = redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(initialStock));
        boolean wasCreated = Boolean.TRUE.equals(created);
        if (wasCreated) {
            log.info("Initialized stock for product {} with {} units", productId.value(), initialStock);
        } else {
            log.info("Stock for product {} already initialized, skipping", productId.value());
        }
        return wasCreated;
    }

    /**
     * Note: the exists-check and the increment are two round-trips, not one
     * atomic operation. That is an acceptable trade-off here because this is
     * an admin/low-frequency path (replenishing stock), never the hot
     * reservation path where 10k concurrent requests would make such a race
     * matter.
     */
    @Override
    public long replenish(ProductId productId, long additionalUnits) {
        String key = stockKey(productId);
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            throw new IllegalStateException(
                    "Cannot replenish product %s: it was never initialized".formatted(productId.value()));
        }
        Long newTotal = redisTemplate.opsForValue().increment(key, additionalUnits);
        log.info("Replenished product {} by {} units (new total: {})", productId.value(), additionalUnits, newTotal);
        return newTotal;
    }

    private String stockKey(ProductId productId) {
        return KEY_PREFIX + productId.value();
    }
}
