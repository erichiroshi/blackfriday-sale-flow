package com.erichiroshi.blackfridaysaleflow.sale.application.port.in;

import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.ProductAlreadyExistsException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;

/**
 * Driver port for admin/operational stock management: creating a product's
 * initial stock counter and replenishing it while the sale is live. Kept
 * separate from {@link ReserveStockUseCase} on purpose — different actor
 * (an operator, not a shopper), different concerns.
 */
public interface ManageProductStockUseCase {

    /**
     * @throws ProductAlreadyExistsException if the product was already initialized.
     */
    void createProduct(ProductId productId, long initialStock);

    /**
     * @return the new total stock after replenishment.
     */
    long replenishStock(ProductId productId, long additionalUnits);
}
