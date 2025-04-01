package com.github.kiraruto.sistemaBancario.repository;

import com.github.kiraruto.sistemaBancario.model.ScheduledTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionScheduleRepository extends JpaRepository<ScheduledTransfer, UUID> {

    @Query("SELECT t FROM ScheduledTransfer t WHERE t.sourceAccount = :accountId AND t.scheduledDate >= :localDateTime")
    List<ScheduledTransfer> findLastHourTransactions(@Param("accountId") UUID accountId, @Param("localDateTime") LocalDateTime localDateTime);
}
