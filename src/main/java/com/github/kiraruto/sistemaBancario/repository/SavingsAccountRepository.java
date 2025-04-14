package com.github.kiraruto.sistemaBancario.repository;

import com.github.kiraruto.sistemaBancario.dto.BalanceDTO;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, UUID> {
    Optional<SavingsAccount> findByCpfAndEmail(String cpf, String email);

    boolean existsByFullNameAndEmailAndCpf(String fullName, String email, String cpf);

    BalanceDTO findFullNameAndBalanceById(UUID uuid);

    UUID findIdByFullNameAndBalance(String s, BigDecimal balance);
}
