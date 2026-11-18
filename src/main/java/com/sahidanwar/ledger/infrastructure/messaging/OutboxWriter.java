package com.sahidanwar.ledger.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahidanwar.ledger.domain.OutboxEvent;
import com.sahidanwar.ledger.domain.Transfer;
import com.sahidanwar.ledger.infrastructure.persistence.OutboxRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxWriter {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxWriter(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void enqueue(Transfer transfer, String eventType) {
        UUID eventId = UUID.randomUUID();
        PaymentEventPayload payload = new PaymentEventPayload(
                eventId,
                eventType,
                transfer.getCreatedAt(),
                transfer.getId(),
                transfer.getFromAccountId(),
                transfer.getToAccountId(),
                transfer.getAmount().toPlainString(),
                transfer.getCurrency(),
                transfer.getStatus().name(),
                transfer.getRiskFlags(),
                transfer.getFailureReason()
        );
        outboxRepository.save(OutboxEvent.payment(eventId, transfer.getId(), eventType, write(payload), Instant.now()));
    }

    private String write(PaymentEventPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize payment event", ex);
        }
    }

    public record PaymentEventPayload(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            UUID transferId,
            UUID fromAccountId,
            UUID toAccountId,
            String amount,
            String currency,
            String status,
            List<String> riskFlags,
            String failureReason
    ) {
    }
}
