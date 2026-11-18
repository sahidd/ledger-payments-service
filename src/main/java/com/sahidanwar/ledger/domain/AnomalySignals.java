package com.sahidanwar.ledger.domain;

import java.math.BigDecimal;

public record AnomalySignals(
        BigDecimal amount,
        long recentCompletedCount,
        boolean beneficiarySeen,
        BigDecimal historicalAverage,
        long historicalCount
) {
}
