package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.config;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.GetOrderStatusUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.PersistOrderBatchUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ReserveStockUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.IdempotencyPort;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.OrderPersistencePort;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.OrderQueuePort;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.StockCachePort;
import com.erichiroshi.blackfridaysaleflow.sale.application.usecase.GetOrderStatusUseCaseImpl;
import com.erichiroshi.blackfridaysaleflow.sale.application.usecase.PersistOrderBatchUseCaseImpl;
import com.erichiroshi.blackfridaysaleflow.sale.application.usecase.ReserveStockUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Explicit wiring for the application layer. Use case implementations are
 * deliberately plain Java classes with no Spring annotations — this class
 * is where Spring meets the use cases, not the use cases themselves.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ReserveStockUseCase reserveStockUseCase(StockCachePort stockCachePort,
                                                    IdempotencyPort idempotencyPort,
                                                    OrderQueuePort orderQueuePort,
                                                    OrderPersistencePort orderPersistencePort,
                                                    Clock clock) {
        return new ReserveStockUseCaseImpl(stockCachePort, idempotencyPort, orderQueuePort, orderPersistencePort, clock);
    }

    @Bean
    public GetOrderStatusUseCase getOrderStatusUseCase(OrderPersistencePort orderPersistencePort) {
        return new GetOrderStatusUseCaseImpl(orderPersistencePort);
    }

    @Bean
    public PersistOrderBatchUseCase persistOrderBatchUseCase(OrderPersistencePort orderPersistencePort,
                                                               StockCachePort stockCachePort) {
        return new PersistOrderBatchUseCaseImpl(orderPersistencePort, stockCachePort);
    }
}
