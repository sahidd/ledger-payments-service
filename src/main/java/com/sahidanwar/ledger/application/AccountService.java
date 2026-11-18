package com.sahidanwar.ledger.application;

import com.sahidanwar.ledger.api.dto.AccountResponse;
import com.sahidanwar.ledger.api.dto.CreateAccountRequest;
import com.sahidanwar.ledger.domain.Account;
import com.sahidanwar.ledger.domain.Amounts;
import com.sahidanwar.ledger.domain.DomainException;
import com.sahidanwar.ledger.domain.JournalEntry;
import com.sahidanwar.ledger.infrastructure.persistence.AccountRepository;
import com.sahidanwar.ledger.infrastructure.persistence.JournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;

    public AccountService(AccountRepository accountRepository, JournalEntryRepository journalEntryRepository) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    @Transactional
    public AccountResponse open(CreateAccountRequest request) {
        BigDecimal initial = Amounts.scale(request.initialBalance());
        Account account = Account.open(request.ownerName().trim(), request.currency(), initial);
        accountRepository.save(account);
        if (initial.signum() > 0) {
            journalEntryRepository.save(JournalEntry.opening(account.getId(), initial, account.getCurrency()));
        }
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID id) {
        return accountRepository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new DomainException("not-found", "Account was not found."));
    }
}
