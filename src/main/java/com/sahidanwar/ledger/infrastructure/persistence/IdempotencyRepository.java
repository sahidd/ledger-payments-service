package com.sahidanwar.ledger.infrastructure.persistence;

import com.sahidanwar.ledger.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {
}
