package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.web;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.RefundOrderUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.InvalidOrderStateTransitionException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.RecordNotFoundException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RefundController.class)
class RefundControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RefundOrderUseCase refundOrderUseCase;

    @Test
    void returns200WhenRefundSucceeds() throws Exception {
        OrderId orderId = OrderId.newId();

        mockMvc.perform(post("/orders/{orderId}/refund", orderId))
                .andExpect(status().isOk());
    }

    @Test
    void returns404WhenOrderNotFound() throws Exception {
        OrderId orderId = OrderId.newId();
        doThrow(new RecordNotFoundException("Order not found"))
                .when(refundOrderUseCase).refund(orderId);

        mockMvc.perform(post("/orders/{orderId}/refund", orderId))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns409WhenOrderIsNotConfirmed() throws Exception {
        OrderId orderId = OrderId.newId();
        doThrow(new InvalidOrderStateTransitionException("Cannot refund order: expected CONFIRMED"))
                .when(refundOrderUseCase).refund(orderId);

        mockMvc.perform(post("/orders/{orderId}/refund", orderId))
                .andExpect(status().isConflict());
    }
}
