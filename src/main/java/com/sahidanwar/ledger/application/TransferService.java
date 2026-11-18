package com.sahidanwar.ledger.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sahidanwar.ledger.api.dto.TransferRequest;
import com.sahidanwar.ledger.api.dto.TransferResponse;
import com.sahidanwar.ledger.config.RiskProperties;
import com.sahidanwar.ledger.domain.Account;
import com.sahidanwar.ledger.domain.AccountStatus;
import com.sahidanwar.ledger.domain.Amounts;
import com.sahidanwar.ledger.domain.AnomalyDetector;
import com.sahidanwar.ledger.domain.AnomalySignals;
import com.sahidanwar.ledger.domain.DomainException;
import com.sahidanwar.ledger.domain.GatewayDecision;
import com.sahidanwar.ledger.domain.GatewayRequest;
import com.sahidanwar.ledger.domain.IdempotencyRecord;
import com.sahidanwar.ledger.domain.JournalEntry;
import com.sahidanwar.ledger.domain.PaymentGateway;
import com.sahidanwar.ledger.domain.Transfer;
import com.sahidanwar.ledger.domain.TransferStatus;
import com.sahidanwar.ledger.infrastructure.messaging.OutboxWriter;
import com.sahidanwar.ledger.infrastructure.persistence.AccountRepository;
import com.sahidanwar.ledger.infrastructure.persistence.IdempotencyRepository;
import com.sahidanwar.ledger.infrastructure.persistence.JournalEntryRepository;
import com.sahidanwar.ledger.infrastructure.persistence.TransferRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;

@Service
public class TransferService {

    private static final URI TRANSFERS = URI.create("/api/transfers");
    private static final int LOCK_ATTEMPTS = 12;

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final OutboxWriter outboxWriter;
    private final PaymentGateway paymentGateway;
    private final AnomalyDetector anomalyDetector;
    private final RiskProperties riskProperties;
    private final ProblemResponses problems;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public TransferService(AccountRepository accountRepository,
                           TransferRepository transferRepository,
                           JournalEntryRepository journalEntryRepository,
                           IdempotencyRepository idempotencyRepository,
                           OutboxWriter outboxWriter,
                           PaymentGateway paymentGateway,
                           AnomalyDetector anomalyDetector,
                           RiskProperties riskProperties,
                           ProblemResponses problems,
                           ObjectMapper objectMapper,
                           PlatformTransactionManager transactionManager) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.outboxWriter = outboxWriter;
        this.paymentGateway = paymentGateway;
        this.anomalyDetector = anomalyDetector;
        this.riskProperties = riskProperties;
        this.problems = problems;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public HttpResult transfer(String idempotencyKey, TransferRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 120) {
            throw new DomainException("invalid-idempotency-key", "Idempotency-Key must be between 1 and 120 characters.");
        }
        String hash = RequestHasher.hash(request.fromAccountId(), request.toAccountId(), request.amount(),
                request.currency(), request.reference());
        HttpResult replay = readReplay(idempotencyKey, hash);
        if (replay != null) {
            return replay;
        }
        int attempt = 0;
        while (true) {
            try {
                return transactionTemplate.execute(status -> executeNew(idempotencyKey, hash, request));
            } catch (RuntimeException ex) {
                if (isOptimisticLock(ex)) {
                    if (++attempt >= LOCK_ATTEMPTS) {
                        throw new DomainException("concurrent-modification",
                                "The account was updated concurrently. Send the request again.");
                    }
                    continue;
                }
                if (isIdempotencyViolation(ex)) {
                    HttpResult raced = readReplay(idempotencyKey, hash);
                    if (raced != null) {
                        return raced;
                    }
                    throw new DomainException("idempotency-conflict", "This idempotency key is already in use.");
                }
                throw ex;
            }
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public TransferResponse get(java.util.UUID id) {
        return transferRepository.findById(id)
                .map(TransferResponse::from)
                .orElseThrow(() -> new DomainException("not-found", "Transaction was not found."));
    }

    private HttpResult executeNew(String key, String hash, TransferRequest request) {
        IdempotencyRecord record = new IdempotencyRecord(key, hash, Instant.now());
        idempotencyRepository.saveAndFlush(record);
        try {
            HttpResult result = post(key, request);
            record.complete(result.status(), objectMapper.writeValueAsString(result.body()));
            return result;
        } catch (DomainException ex) {
            HttpResult result = problems.result(ex, TRANSFERS);
            try {
                record.complete(result.status(), objectMapper.writeValueAsString(result.body()));
            } catch (JsonProcessingException json) {
                throw new IllegalStateException(json);
            }
            return result;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private HttpResult post(String idempotencyKey, TransferRequest request) {
        Account from = accountRepository.findById(request.fromAccountId())
                .orElseThrow(() -> new DomainException("not-found", "Source account was not found."));
        Account to = accountRepository.findById(request.toAccountId())
                .orElseThrow(() -> new DomainException("not-found", "Destination account was not found."));
        if (from.getId().equals(to.getId())) {
            throw new DomainException("same-account", "Transfers must use two different accounts.");
        }
        if (from.getStatus() != AccountStatus.ACTIVE || to.getStatus() != AccountStatus.ACTIVE) {
            throw new DomainException("account-not-active", "Both accounts must be active.");
        }
        if (!from.getCurrency().equals(request.currency()) || !to.getCurrency().equals(request.currency())) {
            throw new DomainException("currency-mismatch", "Transfer currency must match both accounts.");
        }
        BigDecimal amount = Amounts.scale(request.amount());
        if (from.getBalance().compareTo(amount) < 0) {
            throw new DomainException("insufficient-funds", "The source account does not have enough available balance.");
        }

        List<String> flags = anomalyDetector.detect(signals(from, to, amount));
        GatewayDecision decision = paymentGateway.authorize(new GatewayRequest(
                idempotencyKey, from.getId(), to.getId(), amount, request.currency(), request.reference()));

        if (!decision.approved()) {
            Transfer failed = Transfer.failed(from.getId(), to.getId(), amount, request.currency(),
                    request.reference(), flags, decision.reason());
            transferRepository.save(failed);
            outboxWriter.enqueue(failed, "PaymentFailed");
            return created(failed);
        }

        from.debit(amount);
        to.credit(amount);
        Transfer completed = Transfer.completed(from.getId(), to.getId(), amount, request.currency(),
                request.reference(), flags, decision.authId());
        transferRepository.save(completed);
        journalEntryRepository.save(JournalEntry.debit(completed.getId(), from.getId(), amount, request.currency()));
        journalEntryRepository.save(JournalEntry.credit(completed.getId(), to.getId(), amount, request.currency()));
        outboxWriter.enqueue(completed, "PaymentCreated");
        outboxWriter.enqueue(completed, "PaymentCompleted");
        return created(completed);
    }

    private AnomalySignals signals(Account from, Account to, BigDecimal amount) {
        Instant since = Instant.now().minusSeconds(riskProperties.getVelocityWindowSeconds());
        boolean seen = transferRepository.existsByFromAccountIdAndToAccountIdAndStatus(
                from.getId(), to.getId(), TransferStatus.COMPLETED);
        BigDecimal average = transferRepository.averageCompletedAmount(from.getId());
        return new AnomalySignals(
                amount,
                transferRepository.countCompletedSince(from.getId(), since),
                seen,
                average == null ? Amounts.zero() : average,
                transferRepository.countCompleted(from.getId())
        );
    }

    private HttpResult created(Transfer transfer) {
        return new HttpResult(201, objectMapper.valueToTree(TransferResponse.from(transfer)));
    }

    private HttpResult readReplay(String key, String hash) {
        return idempotencyRepository.findById(key).map(record -> {
            if (!record.getRequestHash().equals(hash)) {
                throw new DomainException("idempotency-conflict",
                        "This idempotency key was already used with a different request.");
            }
            if (record.getResponseBody() == null || record.getResponseStatus() == null) {
                throw new DomainException("idempotency-conflict", "This idempotency key is already in progress.");
            }
            try {
                return new HttpResult(record.getResponseStatus(), objectMapper.readTree(record.getResponseBody()));
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException(ex);
            }
        }).orElse(null);
    }

    private boolean isOptimisticLock(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof ObjectOptimisticLockingFailureException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isIdempotencyViolation(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof DataIntegrityViolationException) {
                String message = current.getMessage() == null ? "" : current.getMessage();
                Throwable cause = ((DataIntegrityViolationException) current).getMostSpecificCause();
                String specific = cause.getMessage() == null ? "" : cause.getMessage();
                return message.contains("idempotency_records") || specific.contains("idempotency_records");
            }
            current = current.getCause();
        }
        return false;
    }
}
