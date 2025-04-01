package com.github.kiraruto.sistemaBancario.utils;

import com.github.kiraruto.sistemaBancario.dto.CheckingAccountDTO;
import com.github.kiraruto.sistemaBancario.dto.DepositRequestDTO;
import com.github.kiraruto.sistemaBancario.dto.WithdrawRequestDTO;
import com.github.kiraruto.sistemaBancario.dto.WithdrawalRequestDTO;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CheckingAccountValidate {

    private final CheckingAccountRepository checkingAccountRepository;
    private final UserRepository userRepository;

    public void validateCreateAccountChecking(CheckingAccountDTO checkingAccountDTO) {
        if (checkingAccountRepository.existsByCpfOrDocumentNumber(checkingAccountDTO.cpf(), checkingAccountDTO.documentNumber())) {
            throw new IllegalArgumentException("Conta já existe, não poderá ser criada");
        }

        if (checkingAccountDTO.balance().compareTo(BigDecimal.valueOf(100)) < 0) {
            throw new IllegalArgumentException("O saldo mínimo da conta deve ser de R$100,00.");
        }

        if (checkingAccountDTO.userId() == null) {
            throw new IllegalArgumentException("O usuário não pode ser nulo");
        }

        if (!userRepository.isActiveById(checkingAccountDTO.userId())) {
            throw new IllegalArgumentException("O usuario não está ativo");
        }
    }

    public void validateDepositChecking(DepositRequestDTO depositRequestDTO) {
        if (!checkingAccountRepository.existsByFullName(depositRequestDTO.fullName())) {
            throw new IllegalArgumentException("Não existe conta com este nome");
        }

        if (!checkingAccountRepository.existsByEmail(depositRequestDTO.email())) {
            throw new IllegalArgumentException("Não existe conta com este email");
        }

        if (!checkingAccountRepository.existsByCpf(depositRequestDTO.cpf())) {
            throw new IllegalArgumentException("Não existe conta com este cpf");
        }

        if (!Boolean.TRUE.equals(userRepository.isActiveById(depositRequestDTO.accountSends()))) {
            throw new IllegalArgumentException("O usuario que mandou o dinheiro não está ativo");
        }

        if (!Boolean.TRUE.equals(userRepository.isActiveById(depositRequestDTO.accountReceive()))) {
            throw new IllegalArgumentException("O usuario que recebeu o dinheiro não está ativo");
        }

        if (depositRequestDTO.balance().compareTo(BigDecimal.ZERO) >= 0) {
            throw new IllegalArgumentException("O valor do depósito deve ser positivo.");
        }

        if (!checkingAccountRepository.existsByUserId(depositRequestDTO.userId())) {
            throw new IllegalArgumentException("Não existe conta com este usuario");
        }
    }

    public CheckingAccount validateCheckingAccountWithdraw(@Valid WithdrawRequestDTO withdrawRequestDTO) {
        Optional<CheckingAccount> checkingAccount = checkingAccountRepository.findById(withdrawRequestDTO.idAccount());
        var ch = checkingAccount.get();

        if (!checkingAccountRepository.existsByFullNameAndEmailAndCpf(ch.getFullName(), ch.getEmail(), ch.getCpf())) {
            throw new IllegalArgumentException("Não existe conta com este nome, email ou cpf");
        }

        if (Boolean.FALSE.equals(userRepository.isActiveById(ch.getUser().getId()))) {
            throw new IllegalArgumentException("A conta não está ativa");
        }

        if (withdrawRequestDTO.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do depósito deve ser positivo.");
        }

        return ch;
    }

    public CheckingAccount validateCheckingAccountToFalse(UUID uuid) {
        CheckingAccount checkingAccount = checkingAccountRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("A conta corrente não existe"));

        if (checkingAccount.getIsActive().equals(false)) {
            throw new IllegalArgumentException("A conta já está inativa");
        }

        return checkingAccount;
    }

    public CheckingAccount validateCheckingAccountToTrue(UUID uuid) {
        CheckingAccount checkingAccount = checkingAccountRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("A conta poupança não existe"));

        if (checkingAccount.getIsActive().equals(true)) {
            throw new IllegalArgumentException("A conta já está ativa");
        }

        return checkingAccount;
    }

    public CheckingAccount validateCheckingAccountWithdrawal(@Valid WithdrawalRequestDTO withdrawalRequestDTO) {
        Optional<CheckingAccount> checkingAccount = checkingAccountRepository.findById(withdrawalRequestDTO.idAccount());
        var ch = checkingAccount.get();

        if (!checkingAccountRepository.existsByFullNameAndEmailAndCpf(ch.getFullName(), ch.getEmail(), ch.getCpf())) {
            throw new IllegalArgumentException("Não existe conta com este nome, email ou cpf");
        }

        if (Boolean.FALSE.equals(userRepository.isActiveById(ch.getUser().getId()))) {
            throw new IllegalArgumentException("A conta não está ativa");
        }

        if (withdrawalRequestDTO.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do depósito deve ser positivo.");
        }

        return ch;
    }
}
