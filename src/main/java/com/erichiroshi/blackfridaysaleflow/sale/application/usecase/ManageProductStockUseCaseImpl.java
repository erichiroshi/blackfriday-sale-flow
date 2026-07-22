package com.erichiroshi.blackfridaysaleflow.sale.application.usecase;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ManageProductStockUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.StockCachePort;
import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.ProductAlreadyExistsException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.RecordNotFoundException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;

public final class ManageProductStockUseCaseImpl implements ManageProductStockUseCase {

    private final StockCachePort stockCachePort;

    public ManageProductStockUseCaseImpl(StockCachePort stockCachePort) {
        this.stockCachePort = stockCachePort;
    }

    @Override
    public void createProduct(ProductId productId, long initialStock) {
        if (initialStock <= 0) {
            throw new IllegalArgumentException("initialStock must be positive");
        }
        boolean created = stockCachePort.initialize(productId, initialStock);
        if (!created) {
            throw new ProductAlreadyExistsException(productId);
        }
    }

    @Override
    public long replenishStock(ProductId productId, long additionalUnits) {
        if (additionalUnits <= 0) {
            throw new IllegalArgumentException("additionalUnits must be positive");
        }
        try {
            return stockCachePort.replenish(productId, additionalUnits);
        } catch (IllegalStateException _) {
            throw new RecordNotFoundException(
                    "Product %s does not exist yet — create it first".formatted(productId.value()));
        }
    }
}
