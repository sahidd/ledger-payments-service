package com.sahidanwar.ledger.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Amounts {

    public static final int SCALE = 2;

    private Amounts() {
    }

    public static BigDecimal scale(BigDecimal amount) {
        if (amount == null) {
            throw new DomainException("invalid-amount", "Amount is required.");
        }
        try {
            return amount.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new DomainException("invalid-amount", "Amount supports at most 2 decimal places.");
        }
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, RoundingMode.UNNECESSARY);
    }
}
