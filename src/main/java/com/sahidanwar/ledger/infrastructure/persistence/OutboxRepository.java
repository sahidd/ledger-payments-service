package com.sahidanwar.ledger.infrastructure.persistence;

import com.sahidanwar.ledger.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByAggregateId(UUID aggregateId);

    long countByPublishedAtIsNull();
}
