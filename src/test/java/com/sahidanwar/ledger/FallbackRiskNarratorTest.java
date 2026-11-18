package com.sahidanwar.ledger;

import com.sahidanwar.ledger.application.FallbackRiskNarrator;
import com.sahidanwar.ledger.domain.RiskFlag;
import com.sahidanwar.ledger.domain.Transfer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackRiskNarratorTest {

    @Test
    void explainsEveryFlagInPlainLanguage() {
        Transfer transfer = Transfer.completed(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("6000.00"), "INR", "rent",
                List.of(RiskFlag.UNUSUAL_AMOUNT, RiskFlag.NEW_BENEFICIARY), "auth");

        String explanation = new FallbackRiskNarrator().explain(transfer);

        assertThat(explanation)
                .contains("unusually large")
                .contains("6000.00")
                .contains("has not received a completed transfer");
    }
}
