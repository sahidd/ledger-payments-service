package com.sahidanwar.ledger.infrastructure.persistence;

import com.sahidanwar.ledger.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
}
