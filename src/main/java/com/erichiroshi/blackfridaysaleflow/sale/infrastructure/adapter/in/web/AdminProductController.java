package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.in.web;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.in.ManageProductStockUseCase;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operational/admin endpoints for managing product stock while the sale is
 * live. Deliberately separate from the shopper-facing
 * {@link ReservationController} — different actor, different concerns, and
 * in a real deployment this would sit behind its own authorization rules.
 */
@RestController
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ManageProductStockUseCase manageProductStockUseCase;

    public AdminProductController(ManageProductStockUseCase manageProductStockUseCase) {
        this.manageProductStockUseCase = manageProductStockUseCase;
    }

    @PostMapping
    public ResponseEntity<StockResponseDto> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductId productId = ProductId.of(request.productId());
        manageProductStockUseCase.createProduct(productId, request.initialStock());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new StockResponseDto(productId.value(), request.initialStock()));
    }

    @PatchMapping("/{productId}/stock")
    public ResponseEntity<StockResponseDto> replenish(@PathVariable String productId,
                                                       @Valid @RequestBody ReplenishStockRequest request) {
        ProductId id = ProductId.of(productId);
        long newTotal = manageProductStockUseCase.replenishStock(id, request.additionalUnits());
        return ResponseEntity.ok(new StockResponseDto(id.value(), newTotal));
    }
}
