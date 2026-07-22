package com.erichiroshi.blackfridaysaleflow.sale.domain.model;

import com.erichiroshi.blackfridaysaleflow.sale.domain.exception.InvalidOrderStateTransitionException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private Order newReservedOrder() {
        return Order.reserve(OrderId.newId(), ProductId.of("PRODUCT-1"), CustomerId.of("CUSTOMER-1"),
                IdempotencyKey.of("idem-1"), 1, Instant.now());
    }

    @Test
    void newReservationStartsAsReserved() {
        Order order = newReservedOrder();
        assertThat(order.status()).isEqualTo(OrderStatus.RESERVED);
    }

    @Test
    void confirmTransitionsReservedToConfirmed() {
        Order order = newReservedOrder();
        order.confirm();
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void failTransitionsReservedToFailed() {
        Order order = newReservedOrder();
        order.fail();
        assertThat(order.status()).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    void cannotConfirmAnAlreadyConfirmedOrder() {
        Order order = newReservedOrder();
        order.confirm();
        assertThatThrownBy(order::confirm).isInstanceOf(InvalidOrderStateTransitionException.class);
    }

    @Test
    void cannotFailAnAlreadyConfirmedOrder() {
        Order order = newReservedOrder();
        order.confirm();
        assertThatThrownBy(order::fail).isInstanceOf(InvalidOrderStateTransitionException.class);
    }

    @Test
    void refundTransitionsConfirmedToRefunded() {
        Order order = newReservedOrder();
        order.confirm();
        order.refund();
        assertThat(order.status()).isEqualTo(OrderStatus.REFUNDED);
    }

    @Test
    void cannotRefundAReservedOrder() {
        Order order = newReservedOrder();
        assertThatThrownBy(order::refund).isInstanceOf(InvalidOrderStateTransitionException.class);
    }

    @Test
    void cannotRefundAFailedOrder() {
        Order order = newReservedOrder();
        order.fail();
        assertThatThrownBy(order::refund).isInstanceOf(InvalidOrderStateTransitionException.class);
    }

    @Test
    void cannotRefundAnAlreadyRefundedOrder() {
        Order order = newReservedOrder();
        order.confirm();
        order.refund();
        assertThatThrownBy(order::refund).isInstanceOf(InvalidOrderStateTransitionException.class);
    }

    @Test
    void rejectsNonPositiveQuantity() {
        OrderId orderId = OrderId.newId();
        ProductId productId = ProductId.of("PRODUCT-1");
        CustomerId customerId = CustomerId.of("CUSTOMER-1");
        IdempotencyKey idempotencyKey = IdempotencyKey.of("idem-1");
        Instant now = Instant.now();

        assertThatThrownBy(() -> Order.reserve(orderId, productId,
                customerId, idempotencyKey, 0, now))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
