package com.sahidanwar.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    private UUID id;

    @Column(name = "from_account_id", nullable = false)
    private UUID fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private UUID toAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    @Column(length = 200)
    private String reference;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "risk_flags", nullable = false, length = 500)
    private List<String> riskFlags = new ArrayList<>();

    @Column(name = "gateway_auth_id", length = 120)
    private String gatewayAuthId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Transfer() {
    }

    public static Transfer completed(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String currency,
                                     String reference, List<String> riskFlags, String gatewayAuthId) {
        return build(TransferStatus.COMPLETED, fromAccountId, toAccountId, amount, currency, reference, riskFlags,
                null, gatewayAuthId);
    }

    public static Transfer failed(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String currency,
                                  String reference, List<String> riskFlags, String failureReason) {
        return build(TransferStatus.FAILED, fromAccountId, toAccountId, amount, currency, reference, riskFlags,
                failureReason, null);
    }

    private static Transfer build(TransferStatus status, UUID fromAccountId, UUID toAccountId, BigDecimal amount,
                                  String currency, String reference, List<String> riskFlags, String failureReason,
                                  String gatewayAuthId) {
        Transfer transfer = new Transfer();
        transfer.id = UUID.randomUUID();
        transfer.fromAccountId = fromAccountId;
        transfer.toAccountId = toAccountId;
        transfer.amount = amount;
        transfer.currency = currency;
        transfer.status = status;
        transfer.reference = reference;
        transfer.failureReason = failureReason;
        transfer.riskFlags = new ArrayList<>(riskFlags);
        transfer.gatewayAuthId = gatewayAuthId;
        transfer.createdAt = Instant.now();
        return transfer;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFromAccountId() {
        return fromAccountId;
    }

    public UUID getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getReference() {
        return reference;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public List<String> getRiskFlags() {
        return List.copyOf(riskFlags);
    }

    public String getGatewayAuthId() {
        return gatewayAuthId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
