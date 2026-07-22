package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.database;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.OrderPersistencePort;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class OrderPersistenceAdapter implements OrderPersistencePort {

    private final OrderJpaRepository orderJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    public OrderPersistenceAdapter(OrderJpaRepository orderJpaRepository, JdbcTemplate jdbcTemplate) {
        this.orderJpaRepository = orderJpaRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * All rows are written via a single JDBC batch (one round-trip to the
     * driver, not N), wrapped in one transaction: if any row in the batch
     * fails, everything rolls back so the RabbitMQ consumer can safely
     * retry the whole batch.
     */
    @Override
    @Transactional
    public void saveAll(List<Order> orders) {
        jdbcTemplate.batchUpdate(
                OrderBatchInsertSetter.INSERT_SQL,
                orders,
                orders.size(),
                OrderBatchInsertSetter::bind);
    }

    /**
     * Single-row update (e.g. CONFIRMED -> REFUNDED). Not a hot path, so
     * plain JPA is simpler here than hand-written JDBC.
     */
    @Override
    @Transactional
    public void update(Order order) {
        OrderJpaEntity entity = orderJpaRepository.findById(order.id().value())
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot update order %s: no row found".formatted(order.id())));
        orderJpaRepository.save(OrderJpaMapper.applyDomainStatus(entity, order));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(OrderId orderId) {
        return orderJpaRepository.findById(orderId.value())
                .map(OrderJpaMapper::toDomain);
    }
}
