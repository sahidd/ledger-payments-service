package com.sahidanwar.ledger.application;

import com.sahidanwar.ledger.api.dto.ReconciliationResponse;
import com.sahidanwar.ledger.domain.Account;
import com.sahidanwar.ledger.domain.Amounts;
import com.sahidanwar.ledger.domain.EntryDirection;
import com.sahidanwar.ledger.domain.JournalEntry;
import com.sahidanwar.ledger.domain.Transfer;
import com.sahidanwar.ledger.domain.TransferStatus;
import com.sahidanwar.ledger.infrastructure.persistence.AccountRepository;
import com.sahidanwar.ledger.infrastructure.persistence.JournalEntryRepository;
import com.sahidanwar.ledger.infrastructure.persistence.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReconciliationService {

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final TransferRepository transferRepository;

    public ReconciliationService(AccountRepository accountRepository,
                                 JournalEntryRepository journalEntryRepository,
                                 TransferRepository transferRepository) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.transferRepository = transferRepository;
    }

    @Transactional(readOnly = true)
    public ReconciliationResponse reconcile() {
        List<String> issues = new ArrayList<>();
        for (Account account : accountRepository.findAll()) {
            BigDecimal credits = nullToZero(journalEntryRepository.sumByAccountAndDirection(account.getId(), EntryDirection.CREDIT));
            BigDecimal debits = nullToZero(journalEntryRepository.sumByAccountAndDirection(account.getId(), EntryDirection.DEBIT));
            BigDecimal expected = credits.subtract(debits);
            if (expected.compareTo(account.getBalance()) != 0) {
                issues.add("Account " + account.getId() + " balance " + account.getBalance().toPlainString()
                        + " does not match journal net " + expected.toPlainString());
            }
        }
        for (Transfer transfer : transferRepository.findByStatus(TransferStatus.COMPLETED)) {
            List<JournalEntry> lines = journalEntryRepository.findByTransferId(transfer.getId());
            if (lines.size() != 2) {
                issues.add("Completed transfer " + transfer.getId() + " has " + lines.size() + " journal lines");
                continue;
            }
            BigDecimal debits = sum(lines, EntryDirection.DEBIT);
            BigDecimal credits = sum(lines, EntryDirection.CREDIT);
            if (debits.compareTo(credits) != 0 || debits.compareTo(transfer.getAmount()) != 0) {
                issues.add("Completed transfer " + transfer.getId() + " is not a balanced debit and credit");
            }
        }
        for (Transfer transfer : transferRepository.findByStatus(TransferStatus.FAILED)) {
            if (!journalEntryRepository.findByTransferId(transfer.getId()).isEmpty()) {
                issues.add("Failed transfer " + transfer.getId() + " has journal lines");
            }
        }
        return new ReconciliationResponse(issues.isEmpty(), issues);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? Amounts.zero() : value;
    }

    private BigDecimal sum(List<JournalEntry> lines, EntryDirection direction) {
        return lines.stream()
                .filter(line -> line.getDirection() == direction)
                .map(JournalEntry::getAmount)
                .reduce(Amounts.zero(), BigDecimal::add);
    }
}
