package com.sahidanwar.ledger.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure risk rules. The application service loads the signals; this class does not touch the database.
 */
public final class AnomalyDetector {

    private final BigDecimal unusualAmount;
    private final int velocityLimit;
    private final BigDecimal averageMultiplier;
    private final int minimumSample;

    public AnomalyDetector(BigDecimal unusualAmount, int velocityLimit, BigDecimal averageMultiplier, int minimumSample) {
        this.unusualAmount = unusualAmount;
        this.velocityLimit = velocityLimit;
        this.averageMultiplier = averageMultiplier;
        this.minimumSample = minimumSample;
    }

    public List<String> detect(AnomalySignals signals) {
        List<String> flags = new ArrayList<>();
        if (signals.amount().compareTo(unusualAmount) >= 0 || exceedsHistoricalAverage(signals)) {
            flags.add(RiskFlag.UNUSUAL_AMOUNT);
        }
        if (signals.recentCompletedCount() >= velocityLimit) {
            flags.add(RiskFlag.VELOCITY);
        }
        if (!signals.beneficiarySeen()) {
            flags.add(RiskFlag.NEW_BENEFICIARY);
        }
        return List.copyOf(flags);
    }

    private boolean exceedsHistoricalAverage(AnomalySignals signals) {
        if (signals.historicalCount() < minimumSample || signals.historicalAverage() == null) {
            return false;
        }
        BigDecimal cutoff = signals.historicalAverage().multiply(averageMultiplier);
        return signals.amount().compareTo(cutoff) >= 0;
    }
}
