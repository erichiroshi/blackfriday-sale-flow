package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.web;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.OrderStatusView;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ReservationResponse;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ReserveStockCommand;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.CustomerId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.IdempotencyKey;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;

final class ReservationWebMapper {

    private ReservationWebMapper() {
    }

    static ReserveStockCommand toCommand(String productId, String idempotencyKeyHeader, ReservationRequest request) {
        return new ReserveStockCommand(
                ProductId.of(productId),
                CustomerId.of(request.customerId()),
                IdempotencyKey.of(idempotencyKeyHeader));
    }

    static ReservationResponseDto toDto(ReservationResponse response) {
        String orderId = response.orderId().toString();
        return new ReservationResponseDto(
                orderId,
                response.status().name(),
                response.idempotentReplay(),
                "/orders/%s".formatted(orderId));
    }

    static OrderStatusResponseDto toDto(OrderStatusView view) {
        return new OrderStatusResponseDto(view.orderId().toString(), view.status().name());
    }
}
