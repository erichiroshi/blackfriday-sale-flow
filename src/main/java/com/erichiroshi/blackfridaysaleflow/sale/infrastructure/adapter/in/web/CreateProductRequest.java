package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateProductRequest(
        @NotBlank(message = "productId is required") String productId,
        @Positive(message = "initialStock must be positive") long initialStock) {
}
