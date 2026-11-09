package com.github.kiraruto.sistemaBancario.repository;

import com.github.kiraruto.sistemaBancario.dto.BalanceDTO;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface CheckingAccountRepository extends JpaRepository<CheckingAccount, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CheckingAccount c WHERE c.id = :id")
    Optional<CheckingAccount> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByCpfOrDocumentNumber(String cpf, String s);

    boolean existsByFullName(String s);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    boolean existsByUserId(UUID id);

    Optional<CheckingAccount> findByCpfAndEmail(String cpf, String email);

    BalanceDTO findFullNameAndBalanceById(UUID id);

    boolean existsByFullNameAndEmailAndCpf(String fullName, String email, String cpf);

    UUID findIdByFullNameAndBalance(String s, BigDecimal balance);
}
