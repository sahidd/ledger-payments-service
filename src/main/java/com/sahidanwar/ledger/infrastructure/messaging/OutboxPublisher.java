package com.sahidanwar.ledger.infrastructure.messaging;

import com.sahidanwar.ledger.domain.OutboxEvent;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Publishes unpublished outbox rows to Kafka. Delivery is at-least-once: a crash after the broker
 * accepts the record and before this transaction commits will send the same event again.
 * The event id in the payload is the consumer's deduplication key.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final EntityManager entityManager;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;
    private final String topic;
    private final int batchSize;

    public OutboxPublisher(EntityManager entityManager,
                           KafkaTemplate<String, String> kafkaTemplate,
                           PlatformTransactionManager transactionManager,
                           @Value("${app.kafka.payments-topic}") String topic,
                           @Value("${app.outbox.batch-size}") int batchSize) {
        this.entityManager = entityManager;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.topic = topic;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:1000}")
    public void publish() {
        try {
            transactionTemplate.executeWithoutResult(status -> publishBatch());
        } catch (RuntimeException ex) {
            log.warn("Outbox publish failed: {}", ex.getMessage());
        }
    }

    private void publishBatch() {
        @SuppressWarnings("unchecked")
        List<OutboxEvent> events = entityManager.createNativeQuery("""
                SELECT * FROM outbox_events
                WHERE published_at IS NULL
                ORDER BY created_at
                FOR UPDATE SKIP LOCKED
                LIMIT %d
                """.formatted(batchSize), OutboxEvent.class)
                .getResultList();
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(topic, event.getAggregateId().toString(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
            } catch (Exception ex) {
                throw new IllegalStateException("Kafka send failed for " + event.getId(), ex);
            }
            event.markPublished(Instant.now());
        }
        if (!events.isEmpty()) {
            log.info("Published {} outbox event(s)", events.size());
        }
    }
}
