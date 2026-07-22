package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.database;

import com.erichiroshi.blackfridaysaleflow.sale.application.port.out.OrderPersistencePort;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.Order;
import com.erichiroshi.blackfridaysaleflow.sale.domain.model.OrderId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OrderPersistenceAdapter implements OrderPersistencePort {

    private final OrderJpaRepository orderJpaRepository;

    public OrderPersistenceAdapter(OrderJpaRepository orderJpaRepository) {
        this.orderJpaRepository = orderJpaRepository;
    }

    /**
     * All 200 (or however many) rows are written in a single transaction.
     * If the driver throws mid-batch, Spring rolls back the whole thing —
     * that is precisely what lets the RabbitMQ container retry the batch
     * safely without risking a half-persisted state.
     */
    @Override
    @Transactional
    public void saveAll(List<Order> orders) {
        List<OrderJpaEntity> entities = orders.stream()
                .map(OrderJpaMapper::toEntity)
                .toList();
        orderJpaRepository.saveAll(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(OrderId orderId) {
        return orderJpaRepository.findById(orderId.value())
                .map(OrderJpaMapper::toDomain);
    }
}
