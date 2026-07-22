package com.erichiroshi.blackfridaysaleflow.sale.infrastructure.adapter.out.database;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {
}
