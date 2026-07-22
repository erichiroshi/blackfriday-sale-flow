package com.erichiroshi.blackfridaysaleflow.sale.domain.exception;

import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;

public class ProductAlreadyExistsException extends BusinessException {

    public ProductAlreadyExistsException(ProductId productId) {
        super("Product %s already has stock initialized".formatted(productId.value()));
    }
}
