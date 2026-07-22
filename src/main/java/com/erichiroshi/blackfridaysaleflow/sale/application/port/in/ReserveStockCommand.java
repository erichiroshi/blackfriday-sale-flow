package com.erichiroshi.blackfridaysaleflow.sale.application.port.in;

import com.erichiroshi.blackfridaysaleflow.sale.domain.model.CustomerId;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.IdempotencyKey;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;

/**
 * Input command for {@link ReserveStockUseCase}. Lives in application/port/in
 * so the driving adapter (web controller) never passes its own request DTO
 * straight into the use case.
 */
public record ReserveStockCommand(ProductId productId, CustomerId customerId, IdempotencyKey idempotencyKey) {
}
