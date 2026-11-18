package com.sahidanwar.ledger;

import com.sahidanwar.ledger.domain.AnomalyDetector;
import com.sahidanwar.ledger.domain.AnomalySignals;
import com.sahidanwar.ledger.domain.RiskFlag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalyDetectorTest {

    private final AnomalyDetector detector = new AnomalyDetector(
            new BigDecimal("5000.00"), 5, new BigDecimal("5"), 3);

    @Test
    void flagsAnAmountAboveTheAbsoluteThreshold() {
        assertThat(detector.detect(signals("6000.00", 0, true, "10.00", 0)))
                .contains(RiskFlag.UNUSUAL_AMOUNT)
                .doesNotContain(RiskFlag.VELOCITY);
    }

    @Test
    void flagsAnAmountThatIsManyTimesTheHistoricalAverage() {
        assertThat(detector.detect(signals("500.00", 0, true, "40.00", 4)))
                .contains(RiskFlag.UNUSUAL_AMOUNT);
    }

    @Test
    void ignoresAThinHistoryWhenTheAbsoluteThresholdIsNotMet() {
        assertThat(detector.detect(signals("500.00", 0, true, "10.00", 2)))
                .doesNotContain(RiskFlag.UNUSUAL_AMOUNT);
    }

    @Test
    void flagsVelocityAndANewBeneficiary() {
        assertThat(detector.detect(signals("10.00", 5, false, "10.00", 5)))
                .containsExactly(RiskFlag.VELOCITY, RiskFlag.NEW_BENEFICIARY);
    }

    private AnomalySignals signals(String amount, long recent, boolean seen, String average, long history) {
        return new AnomalySignals(new BigDecimal(amount), recent, seen, new BigDecimal(average), history);
    }
}
