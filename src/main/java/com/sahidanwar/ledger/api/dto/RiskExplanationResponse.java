package com.sahidanwar.ledger.api.dto;

import java.util.List;
import java.util.UUID;

public record RiskExplanationResponse(
        UUID transactionId,
        List<String> riskFlags,
        String explanation,
        String source
) {
}
