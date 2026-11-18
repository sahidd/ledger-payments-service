package com.sahidanwar.ledger.application;

import com.sahidanwar.ledger.domain.RiskFlag;
import com.sahidanwar.ledger.domain.Transfer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FallbackRiskNarrator {

    public String explain(Transfer transfer) {
        List<String> reasons = new ArrayList<>();
        for (String flag : transfer.getRiskFlags()) {
            reasons.add(switch (flag) {
                case RiskFlag.UNUSUAL_AMOUNT -> "the amount " + transfer.getAmount().toPlainString() + " "
                        + transfer.getCurrency() + " is unusually large for this account";
                case RiskFlag.VELOCITY -> "the source account has sent many transfers in a short window";
                case RiskFlag.NEW_BENEFICIARY ->
                        "the beneficiary has not received a completed transfer from this account before";
                default -> "rule " + flag + " matched";
            });
        }
        return "This transaction was flagged because " + String.join("; ", reasons) + ".";
    }
}
