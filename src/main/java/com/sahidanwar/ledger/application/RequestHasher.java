package com.sahidanwar.ledger.application;

import com.sahidanwar.ledger.domain.Amounts;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public final class RequestHasher {

    private RequestHasher() {
    }

    public static String hash(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String currency, String reference) {
        String canonical = fromAccountId
                + "|" + toAccountId
                + "|" + Amounts.scale(amount).toPlainString()
                + "|" + currency
                + "|" + (reference == null ? "" : reference);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
