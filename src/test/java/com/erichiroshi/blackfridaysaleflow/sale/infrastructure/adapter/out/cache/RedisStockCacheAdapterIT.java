package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.cache;

import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.StockReservationResult;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the core correctness property of this whole architecture: with 100
 * units of stock and far more concurrent callers, exactly 100 reservations
 * succeed and the Redis counter never goes negative — even though a plain
 * DECR (without the Lua compensation) would race past zero under this load.
 */
@Testcontainers
class RedisStockCacheAdapterIT {

    private static final int INITIAL_STOCK = 100;
    private static final int CONCURRENT_REQUESTS = 500;

    @Container
    static RedisContainer redis =
            new RedisContainer(DockerImageName.parse("redis:8.6.2-alpine"));

    private StringRedisTemplate redisTemplate;
    private RedisStockCacheAdapter adapter;
    private ProductId productId;

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                redis.getHost(), redis.getMappedPort(6379));
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/reserve_stock.lua")));
        script.setResultType(Long.class);

        adapter = new RedisStockCacheAdapter(redisTemplate, script);

        productId = ProductId.of("PRODUCT-1");
        redisTemplate.opsForValue().set("stock:" + productId.value(), String.valueOf(INITIAL_STOCK));
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete("stock:" + productId.value());
    }

    @Test
    void neverOversellsUnderHighConcurrency() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_REQUESTS);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger outOfStockCount = new AtomicInteger();

        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    StockReservationResult result = adapter.reserve(productId);
                    if (result.successful()) {
                        successCount.incrementAndGet();
                    } else {
                        outOfStockCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK);
        assertThat(outOfStockCount.get()).isEqualTo(CONCURRENT_REQUESTS - INITIAL_STOCK);

        String remaining = redisTemplate.opsForValue().get("stock:" + productId.value());
        assertThat(remaining).isEqualTo("0");
    }

    @Test
    void releaseIncrementsCounterBackForCompensation() {
        adapter.reserve(productId);
        adapter.reserve(productId);

        adapter.release(productId);

        String remaining = redisTemplate.opsForValue().get("stock:" + productId.value());
        assertThat(remaining).isEqualTo(String.valueOf(INITIAL_STOCK - 1));
    }
}
