package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record ReservationRequest(
        @NotBlank(message = "customerId is required") String customerId) {
}
