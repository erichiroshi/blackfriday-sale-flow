package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.web;

public record ReservationResponseDto(
        String orderId,
        String status,
        boolean idempotentReplay,
        String statusUrl) {
}
