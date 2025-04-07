package com.github.kiraruto.sistemaBancario.repository;

import com.github.kiraruto.sistemaBancario.dto.BalanceDTO;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, UUID> {
    Optional<SavingsAccount> findByCpfAndEmail(String cpf, String email);

    boolean existsByFullNameAndEmailAndCpf(String fullName, String email, String cpf);

    BalanceDTO findFullNameAndBalanceById(UUID uuid);

    UUID findIdByFullNameAndBalance(String s, BigDecimal balance);
}
