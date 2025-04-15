package com.github.kiraruto.sistemaBancario.repository;

import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findLast24HoursByAccountSends(UUID accountSends);

    @Query("SELECT t FROM Transaction t WHERE (t.accountReceive = :uuid OR t.accountSends = :uuid) AND t.transactionDate >= :dataLimite")
    List<Transaction> findLasts24Hours(@Param("uuid") UUID uuid, @Param("dataLimite") LocalDateTime dataLimite);

    List<Transaction> findByAccountSends(UUID uuid);

    @Query("SELECT t FROM Transaction t where t.status = :status AND (t.accountReceive = :uuid OR t.accountSends = :uuid)")
    List<Transaction> findAllByIdTypePendente(@Param("uuid") UUID uuid, @Param("status") EnumStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.accountSends = :accountId " +
            "AND t.transactionType = :transactionType " +
            "AND t.transactionDate BETWEEN :start AND :end")
    Optional<BigDecimal> sumWithdrawalsByAccountAndDateTimeRange(@Param("accountId") UUID accountId,
                                                                 @Param("transactionType") EnumTransactionType transactionType,
                                                                 @Param("start") LocalDateTime start,
                                                                 @Param("end") LocalDateTime end);
}
