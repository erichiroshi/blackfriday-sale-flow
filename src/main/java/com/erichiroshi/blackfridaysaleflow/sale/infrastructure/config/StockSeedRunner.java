package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Seeds the Redis stock counter for the demo product at startup, only if it
 * does not already exist (so restarting the app mid-sale never resets the
 * counter back to full stock).
 */
@Component
public class StockSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StockSeedRunner.class);
    private static final String KEY_PREFIX = "stock:";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.demo.product-id:PRODUCT-1}")
    private String demoProductId;

    @Value("${app.demo.initial-stock:100}")
    private long initialStock;

    public StockSeedRunner(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String key = KEY_PREFIX + demoProductId;
        Boolean seeded = redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(initialStock));
        if (Boolean.TRUE.equals(seeded)) {
            log.info("Seeded stock for product {} with {} units", demoProductId, initialStock);
        } else {
            log.info("Stock key {} already present, skipping seed", key);
        }
    }
}
