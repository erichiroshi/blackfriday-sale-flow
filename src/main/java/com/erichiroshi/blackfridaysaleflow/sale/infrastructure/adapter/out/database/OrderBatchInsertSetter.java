package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.database;

import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Binds a single {@link Order} into the parameters of the batch INSERT
 * PreparedStatement. Kept separate from the adapter class so the SQL
 * parameter mapping has one clear, testable place to live.
 */
final class OrderBatchInsertSetter {

    static final String INSERT_SQL = """
            INSERT INTO orders (id, product_id, customer_id, idempotency_key, quantity, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private OrderBatchInsertSetter() {
    }

    static void bind(PreparedStatement ps, Order order) throws SQLException {
        ps.setObject(1, order.id().value());
        ps.setString(2, order.productId().value());
        ps.setString(3, order.customerId().value());
        ps.setString(4, order.idempotencyKey().value());
        ps.setInt(5, order.quantity());
        ps.setString(6, order.status().name());
        ps.setTimestamp(7, Timestamp.from(order.createdAt()));
    }
}
