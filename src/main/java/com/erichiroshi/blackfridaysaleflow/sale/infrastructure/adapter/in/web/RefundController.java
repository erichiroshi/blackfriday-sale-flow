package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.web;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.RefundOrderUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders/{orderId}/refund")
public class RefundController {

    private final RefundOrderUseCase refundOrderUseCase;

    public RefundController(RefundOrderUseCase refundOrderUseCase) {
        this.refundOrderUseCase = refundOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> refund(@PathVariable String orderId) {
        refundOrderUseCase.refund(OrderId.of(orderId));
        return ResponseEntity.ok().build();
    }
}
