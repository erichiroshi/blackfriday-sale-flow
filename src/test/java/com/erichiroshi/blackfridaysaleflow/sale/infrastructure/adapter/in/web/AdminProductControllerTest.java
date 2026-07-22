package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.web;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ManageProductStockUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.ProductAlreadyExistsException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.RecordNotFoundException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductController.class)
class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private ManageProductStockUseCase manageProductStockUseCase;

    @Test
    void returns201WhenProductCreated() throws Exception {
        mockMvc.perform(post("/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest("TV", 1200))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value("TV"))
                .andExpect(jsonPath("$.totalStock").value(1200));
    }

    @Test
    void returns409WhenProductAlreadyExists() throws Exception {
        doThrow(new ProductAlreadyExistsException(ProductId.of("TV")))
                .when(manageProductStockUseCase).createProduct(eq(ProductId.of("TV")), eq(1200L));

        mockMvc.perform(post("/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest("TV", 1200))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PRODUCT_ALREADY_EXISTS"));
    }

    @Test
    void returns400WhenInitialStockIsNotPositive() throws Exception {
        mockMvc.perform(post("/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest("TV", 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns200WithNewTotalWhenReplenished() throws Exception {
        when(manageProductStockUseCase.replenishStock(ProductId.of("PC"), 100)).thenReturn(1550L);

        mockMvc.perform(patch("/admin/products/{productId}/stock", "PC")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReplenishStockRequest(100))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStock").value(1550));
    }

    @Test
    void returns404WhenReplenishingUnknownProduct() throws Exception {
        when(manageProductStockUseCase.replenishStock(ProductId.of("UNKNOWN"), 10))
                .thenThrow(new RecordNotFoundException("Product UNKNOWN does not exist yet"));

        mockMvc.perform(patch("/admin/products/{productId}/stock", "UNKNOWN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReplenishStockRequest(10))))
                .andExpect(status().isNotFound());
    }
}
