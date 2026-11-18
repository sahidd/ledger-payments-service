package com.sahidanwar.ledger.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record GatewayRequest(
        String idempotencyKey,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String currency,
        String reference
) {
}
