package com.sahidanwar.ledger.infrastructure.persistence;

import com.sahidanwar.ledger.domain.Transfer;
import com.sahidanwar.ledger.domain.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    List<Transfer> findByStatus(TransferStatus status);

    boolean existsByFromAccountIdAndToAccountIdAndStatus(UUID fromAccountId, UUID toAccountId, TransferStatus status);

    @Query("""
            select count(t) from Transfer t
            where t.fromAccountId = :accountId
              and t.status = com.sahidanwar.ledger.domain.TransferStatus.COMPLETED
              and t.createdAt >= :since
            """)
    long countCompletedSince(@Param("accountId") UUID accountId, @Param("since") Instant since);

    @Query("""
            select count(t) from Transfer t
            where t.fromAccountId = :accountId
              and t.status = com.sahidanwar.ledger.domain.TransferStatus.COMPLETED
            """)
    long countCompleted(@Param("accountId") UUID accountId);

    @Query("""
            select coalesce(avg(t.amount), 0) from Transfer t
            where t.fromAccountId = :accountId
              and t.status = com.sahidanwar.ledger.domain.TransferStatus.COMPLETED
            """)
    BigDecimal averageCompletedAmount(@Param("accountId") UUID accountId);
}
