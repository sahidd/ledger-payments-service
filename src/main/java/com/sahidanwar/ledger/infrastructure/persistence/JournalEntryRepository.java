package com.sahidanwar.ledger.infrastructure.persistence;

import com.sahidanwar.ledger.domain.EntryDirection;
import com.sahidanwar.ledger.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    List<JournalEntry> findByTransferId(UUID transferId);

    @Query("""
            select coalesce(sum(j.amount), 0) from JournalEntry j
            where j.accountId = :accountId and j.direction = :direction
            """)
    BigDecimal sumByAccountAndDirection(@Param("accountId") UUID accountId, @Param("direction") EntryDirection direction);
}
