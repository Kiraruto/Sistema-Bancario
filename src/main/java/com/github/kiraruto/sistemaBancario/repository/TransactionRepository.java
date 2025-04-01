package com.github.kiraruto.sistemaBancario.repository;

import com.github.kiraruto.sistemaBancario.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findLast24HoursByAccountSends(UUID accountSends);

    List<Transaction> findByAccountSends(UUID uuid);

    List<Transaction> findAllById(UUID id);
}
