package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds the {@code app.demo.products} list from application.yml — the
 * products seeded at startup (TV, PC, GELADEIRA, ...), each with its own
 * initial stock.
 */
@ConfigurationProperties(prefix = "app.demo")
public class DemoProductsProperties {

    private List<ProductSeed> products = List.of();

    public List<ProductSeed> getProducts() {
        return products;
    }

    public void setProducts(List<ProductSeed> products) {
        this.products = products;
    }

    public record ProductSeed(String id, long initialStock) {
    }
}
