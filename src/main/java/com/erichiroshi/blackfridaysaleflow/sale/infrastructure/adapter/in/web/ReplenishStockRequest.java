package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.web;

import jakarta.validation.constraints.Positive;

public record ReplenishStockRequest(
        @Positive(message = "additionalUnits must be positive") long additionalUnits) {
}
