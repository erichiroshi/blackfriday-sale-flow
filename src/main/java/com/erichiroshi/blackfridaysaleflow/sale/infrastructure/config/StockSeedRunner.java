package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.config;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ManageProductStockUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.ProductAlreadyExistsException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Seeds the demo catalog (TV, PC, GELADEIRA, ... — configured via
 * {@code app.demo.products} in application.yml) at startup, going through
 * the same {@link ManageProductStockUseCase} the admin endpoint uses — no
 * duplicated Redis logic between "seed at boot" and "create via API".
 *
 * Idempotent: relies on the use case's underlying SETNX semantics, so
 * restarting the app mid-sale never resets stock back to full. A product
 * that already exists is logged and skipped, not treated as an error.
 */
@EnableConfigurationProperties(DemoProductsProperties.class)
@Component
public class StockSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StockSeedRunner.class);

    private final ManageProductStockUseCase manageProductStockUseCase;
    private final DemoProductsProperties demoProductsProperties;

    public StockSeedRunner(ManageProductStockUseCase manageProductStockUseCase,
                           DemoProductsProperties demoProductsProperties) {
        this.manageProductStockUseCase = manageProductStockUseCase;
        this.demoProductsProperties = demoProductsProperties;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        for (DemoProductsProperties.ProductSeed seed : demoProductsProperties.getProducts()) {
            seedOne(seed);
        }
    }

    private void seedOne(DemoProductsProperties.ProductSeed seed) {
        try {
            manageProductStockUseCase.createProduct(ProductId.of(seed.id()), seed.initialStock());
            log.info("Seeded stock for product {} with {} units", seed.id(), seed.initialStock());
        } catch (ProductAlreadyExistsException _) {
            log.info("Product {} already has stock initialized, skipping seed", seed.id());
        }
    }
}
