package com.erichiroshi.blackfridaysaleflow.sale.application.usecase;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.StockCachePort;
import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.ProductAlreadyExistsException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.RecordNotFoundException;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageProductStockUseCaseImplTest {

    @Mock
    private StockCachePort stockCachePort;

    @InjectMocks
    private ManageProductStockUseCaseImpl useCase;

    @Test
    void createsProductWhenNotYetInitialized() {
        ProductId productId = ProductId.of("TV");
        when(stockCachePort.initialize(productId, 1200)).thenReturn(true);

        useCase.createProduct(productId, 1200);
        // no exception -> success
        verify(stockCachePort).initialize(productId, 1200);
    }

    @Test
    void throwsWhenProductAlreadyExists() {
        ProductId productId = ProductId.of("TV");
        when(stockCachePort.initialize(productId, 1200)).thenReturn(false);

        assertThatThrownBy(() -> useCase.createProduct(productId, 1200))
                .isInstanceOf(ProductAlreadyExistsException.class);
    }

    @Test
    void rejectsNonPositiveInitialStock() {
        ProductId productId = ProductId.of("TV");
        assertThatThrownBy(() -> useCase.createProduct(productId, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replenishesExistingProduct() {
        ProductId productId = ProductId.of("PC");
        when(stockCachePort.replenish(productId, 100)).thenReturn(1550L);

        long newTotal = useCase.replenishStock(productId, 100);

        assertThat(newTotal).isEqualTo(1550L);
    }

    @Test
    void translatesMissingProductIntoRecordNotFound() {
        ProductId productId = ProductId.of("UNKNOWN");
        when(stockCachePort.replenish(productId, 10))
                .thenThrow(new IllegalStateException("not initialized"));

        assertThatThrownBy(() -> useCase.replenishStock(productId, 10))
                .isInstanceOf(RecordNotFoundException.class);
    }

    @Test
    void rejectsNonPositiveReplenishAmount() {
        ProductId productId = ProductId.of("TV");
        assertThatThrownBy(() -> useCase.replenishStock(productId, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
