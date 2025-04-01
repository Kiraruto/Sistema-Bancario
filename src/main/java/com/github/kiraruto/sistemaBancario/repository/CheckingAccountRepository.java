package com.github.kiraruto.sistemaBancario.repository;

import com.github.kiraruto.sistemaBancario.dto.BalanceDTO;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CheckingAccountRepository extends JpaRepository<CheckingAccount, UUID> {
    boolean existsByCpfOrDocumentNumber(String cpf, String s);

    boolean existsByFullName(String s);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    boolean existsByUserId(UUID id);

    Optional<CheckingAccount> findByCpfAndEmail(String cpf, String email);

    BalanceDTO findFullNameAndBalanceById(UUID id);

    boolean existsByFullNameAndEmailAndCpf(String fullName, String email, String cpf);
}
