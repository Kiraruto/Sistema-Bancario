package com.github.kiraruto.sistemaBancario.repository;

import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findLast24HoursByAccountSends(UUID accountSends);

    @Query("SELECT t FROM Transaction t WHERE (t.accountReceive = :uuid OR t.accountSends = :uuid) AND t.transactionDate >= :dataLimite")
    List<Transaction> findLasts24Hours(@Param("uuid") UUID uuid, @Param("dataLimite") LocalDateTime dataLimite);

    List<Transaction> findByAccountSends(UUID uuid);

    @Query("SELECT t FROM Transaction t where t.status = :status AND (t.accountReceive = :uuid OR t.accountSends = :uuid)")
    List<Transaction> findAllByIdTypePendente(@Param("uuid") UUID uuid, @Param("status") EnumStatus status);
}
