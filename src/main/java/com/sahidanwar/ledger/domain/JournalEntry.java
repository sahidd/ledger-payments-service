package com.sahidanwar.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
public class JournalEntry {

    @Id
    private UUID id;

    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntryDirection direction;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 200)
    private String memo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected JournalEntry() {
    }

    public static JournalEntry opening(UUID accountId, BigDecimal amount, String currency) {
        return create(null, accountId, EntryDirection.CREDIT, amount, currency, "OPENING_BALANCE");
    }

    public static JournalEntry debit(UUID transferId, UUID accountId, BigDecimal amount, String currency) {
        return create(transferId, accountId, EntryDirection.DEBIT, amount, currency, "TRANSFER");
    }

    public static JournalEntry credit(UUID transferId, UUID accountId, BigDecimal amount, String currency) {
        return create(transferId, accountId, EntryDirection.CREDIT, amount, currency, "TRANSFER");
    }

    private static JournalEntry create(UUID transferId, UUID accountId, EntryDirection direction, BigDecimal amount,
                                       String currency, String memo) {
        JournalEntry entry = new JournalEntry();
        entry.id = UUID.randomUUID();
        entry.transferId = transferId;
        entry.accountId = accountId;
        entry.direction = direction;
        entry.amount = amount;
        entry.currency = currency;
        entry.memo = memo;
        entry.createdAt = Instant.now();
        return entry;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public EntryDirection getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getMemo() {
        return memo;
    }
}
