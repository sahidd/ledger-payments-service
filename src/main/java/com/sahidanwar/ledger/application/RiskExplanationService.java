package com.sahidanwar.ledger.application;

import com.sahidanwar.ledger.api.dto.RiskExplanationResponse;
import com.sahidanwar.ledger.domain.DomainException;
import com.sahidanwar.ledger.domain.Transfer;
import com.sahidanwar.ledger.infrastructure.persistence.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RiskExplanationService {

    private static final Logger log = LoggerFactory.getLogger(RiskExplanationService.class);

    private static final String SYSTEM = """
            You explain payment risk flags for a fintech reviewer.
            Use only the flags and facts in the user message.
            Write two or three plain sentences. Do not invent extra flags or account history.
            """;

    private final TransferRepository transferRepository;
    private final FallbackRiskNarrator fallback;
    private final ObjectProvider<TextGenerator> textGenerator;

    public RiskExplanationService(TransferRepository transferRepository,
                                  FallbackRiskNarrator fallback,
                                  ObjectProvider<TextGenerator> textGenerator) {
        this.transferRepository = transferRepository;
        this.fallback = fallback;
        this.textGenerator = textGenerator;
    }

    @Transactional(readOnly = true)
    public RiskExplanationResponse explain(UUID transactionId) {
        Transfer transfer = transferRepository.findById(transactionId)
                .orElseThrow(() -> new DomainException("not-found", "Transaction was not found."));
        if (transfer.getRiskFlags().isEmpty()) {
            return new RiskExplanationResponse(transactionId, transfer.getRiskFlags(),
                    "This transaction was not flagged by the risk rules.", "rules");
        }
        TextGenerator generator = textGenerator.getIfAvailable();
        if (generator != null) {
            try {
                String text = generator.complete(SYSTEM, prompt(transfer));
                if (text != null && !text.isBlank()) {
                    return new RiskExplanationResponse(transactionId, transfer.getRiskFlags(), text.trim(), "openai");
                }
            } catch (RuntimeException ex) {
                log.warn("AI risk explanation failed, using the rule fallback: {}", ex.getMessage());
            }
        }
        return new RiskExplanationResponse(transactionId, transfer.getRiskFlags(), fallback.explain(transfer), "fallback");
    }

    private String prompt(Transfer transfer) {
        return "Transaction " + transfer.getId()
                + " amount " + transfer.getAmount().toPlainString() + " " + transfer.getCurrency()
                + " status " + transfer.getStatus()
                + " flags " + String.join(", ", transfer.getRiskFlags())
                + ". Explain why it was flagged.";
    }
}
