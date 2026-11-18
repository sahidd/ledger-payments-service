package com.sahidanwar.ledger;

import com.sahidanwar.ledger.application.RequestHasher;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RequestHasherTest {

    @Test
    void sameLogicalAmountProducesTheSameHash() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();

        String scaled = RequestHasher.hash(from, to, new BigDecimal("10.50"), "INR", "rent");
        String trailing = RequestHasher.hash(from, to, new BigDecimal("10.5"), "INR", "rent");
        String different = RequestHasher.hash(from, to, new BigDecimal("10.51"), "INR", "rent");

        assertThat(scaled).isEqualTo(trailing).hasSize(64);
        assertThat(different).isNotEqualTo(scaled);
    }
}
