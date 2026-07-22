package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.web;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ReservationResponse;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ReserveStockCommand;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ReserveStockUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Synchronous flow entry point. Talks only to {@link ReserveStockUseCase} —
 * never to Redis, RabbitMQ, or Postgres directly. Response is 202 Accepted:
 * the reservation in Redis is confirmed, but Postgres persistence happens
 * later, asynchronously, in the batch worker.
 */
@RestController
@RequestMapping("/products/{productId}/reservations")
public class ReservationController {

    private final ReserveStockUseCase reserveStockUseCase;

    public ReservationController(ReserveStockUseCase reserveStockUseCase) {
        this.reserveStockUseCase = reserveStockUseCase;
    }

    @PostMapping
    public ResponseEntity<ReservationResponseDto> reserve(
            @PathVariable String productId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReservationRequest request) {

        ReserveStockCommand command = ReservationWebMapper.toCommand(productId, idempotencyKey, request);
        ReservationResponse response = reserveStockUseCase.reserve(command);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ReservationWebMapper.toDto(response));
    }
}
