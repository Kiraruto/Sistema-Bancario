package com.github.kiraruto.sistemaBancario.repository;

import com.github.kiraruto.sistemaBancario.dto.BalanceDTO;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SavingsAccount s WHERE s.id = :id")
    Optional<SavingsAccount> findByIdForUpdate(@Param("id") UUID id);

    Optional<SavingsAccount> findByCpfAndEmail(String cpf, String email);

    boolean existsByFullNameAndEmailAndCpf(String fullName, String email, String cpf);

    BalanceDTO findFullNameAndBalanceById(UUID uuid);

    UUID findIdByFullNameAndBalance(String s, BigDecimal balance);
}
