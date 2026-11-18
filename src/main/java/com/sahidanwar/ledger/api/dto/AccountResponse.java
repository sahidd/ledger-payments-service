package com.sahidanwar.ledger.api.dto;

import com.sahidanwar.ledger.domain.Account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String ownerName,
        String currency,
        BigDecimal balance,
        String status,
        Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getOwnerName(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus().name(),
                account.getCreatedAt()
        );
    }
}
