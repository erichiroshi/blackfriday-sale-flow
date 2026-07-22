package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.web;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.GetOrderStatusUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.OrderStatusView;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderStatusController {

    private final GetOrderStatusUseCase getOrderStatusUseCase;

    public OrderStatusController(GetOrderStatusUseCase getOrderStatusUseCase) {
        this.getOrderStatusUseCase = getOrderStatusUseCase;
    }

    @GetMapping("/{orderId}")
    public OrderStatusResponseDto getStatus(@PathVariable String orderId) {
        OrderStatusView view = getOrderStatusUseCase.getStatus(OrderId.of(orderId));
        return ReservationWebMapper.toDto(view);
    }
}
