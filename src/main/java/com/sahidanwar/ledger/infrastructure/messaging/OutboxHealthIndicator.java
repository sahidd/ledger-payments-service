package com.sahidanwar.ledger.infrastructure.messaging;

import com.sahidanwar.ledger.infrastructure.persistence.OutboxRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class OutboxHealthIndicator implements HealthIndicator {

    private final OutboxRepository outboxRepository;

    public OutboxHealthIndicator(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Override
    public Health health() {
        long pending = outboxRepository.countByPublishedAtIsNull();
        Health.Builder builder = pending > 1000 ? Health.down() : Health.up();
        return builder.withDetail("pending", pending).build();
    }
}
