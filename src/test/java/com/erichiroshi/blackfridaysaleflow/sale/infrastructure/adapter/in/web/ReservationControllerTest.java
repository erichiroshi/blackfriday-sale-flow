package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.web;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ReservationResponse;
import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ReserveStockUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.OutOfStockException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReserveStockUseCase reserveStockUseCase;

    @Test
    void returns202WithOrderIdWhenReservationSucceeds() throws Exception {
        OrderId orderId = OrderId.newId();
        when(reserveStockUseCase.reserve(any())).thenReturn(ReservationResponse.newReservation(orderId));

        mockMvc.perform(post("/products/{productId}/reservations", "PRODUCT-1")
                        .header("Idempotency-Key", "client-generated-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReservationRequest("CUSTOMER-1"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.idempotentReplay").value(false));
    }

    @Test
    void returns409WhenProductIsOutOfStock() throws Exception {
        when(reserveStockUseCase.reserve(any())).thenThrow(new OutOfStockException(ProductId.of("PRODUCT-1")));

        mockMvc.perform(post("/products/{productId}/reservations", "PRODUCT-1")
                        .header("Idempotency-Key", "client-generated-key-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReservationRequest("CUSTOMER-1"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("OUT_OF_STOCK"));
    }

    @Test
    void returns400WhenIdempotencyKeyHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/products/{productId}/reservations", "PRODUCT-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReservationRequest("CUSTOMER-1"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns400WhenCustomerIdIsBlank() throws Exception {
        mockMvc.perform(post("/products/{productId}/reservations", "PRODUCT-1")
                        .header("Idempotency-Key", "client-generated-key-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReservationRequest(""))))
                .andExpect(status().isBadRequest());
    }
}
