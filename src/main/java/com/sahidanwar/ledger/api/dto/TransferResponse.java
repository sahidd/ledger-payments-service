package com.sahidanwar.ledger.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sahidanwar.ledger.domain.Transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransferResponse(
        UUID id,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String currency,
        String status,
        List<String> riskFlags,
        String failureReason,
        String gatewayAuthId,
        Instant createdAt
) {
    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromAccountId(),
                transfer.getToAccountId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus().name(),
                transfer.getRiskFlags(),
                transfer.getFailureReason(),
                transfer.getGatewayAuthId(),
                transfer.getCreatedAt()
        );
    }
}
