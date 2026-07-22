package com.erichiroshi.blackfridaysaleflow.sale.domain.exception;

import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;

public class OutOfStockException extends BusinessException {

    public OutOfStockException(ProductId productId) {
        super("Product %s is out of stock".formatted(productId.value()));
    }
}
