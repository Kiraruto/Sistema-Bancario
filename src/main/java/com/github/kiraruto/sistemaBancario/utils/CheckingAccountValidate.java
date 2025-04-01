package com.github.kiraruto.sistemaBancario.utils;

import com.github.kiraruto.sistemaBancario.dto.CheckingAccountDTO;
import com.github.kiraruto.sistemaBancario.dto.DepositRequestDTO;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
}
