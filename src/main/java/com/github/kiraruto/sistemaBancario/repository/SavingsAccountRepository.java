package com.github.kiraruto.sistemaBancario.repository;

import com.github.kiraruto.sistemaBancario.dto.BalanceDTO;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, UUID> {
    Optional<SavingsAccount> findByCpfAndEmail(String cpf, String email);

    boolean existsByFullNameAndEmailAndCpf(String fullName, String email, String cpf);

    @Query("SELECT sa.balance FROM SavingsAccount sa WHERE sa.id = :id")
    BigDecimal findBalanceById(@Param("id") UUID id);

    BalanceDTO findFullNameAndBalanceById(UUID uuid);

    boolean existsByUserId(UUID id);
}
