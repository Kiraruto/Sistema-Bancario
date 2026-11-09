package com.github.kiraruto.sistemaBancario.utils;

import com.github.kiraruto.sistemaBancario.dto.DepositRequestDTO;
import com.github.kiraruto.sistemaBancario.dto.SavingsAccountDTO;
import com.github.kiraruto.sistemaBancario.dto.WithdrawRequestDTO;
import com.github.kiraruto.sistemaBancario.dto.WithdrawalRequestDTO;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SavingsAccountValidate {

    private final SavingsAccountRepository savingsAccountRepository;
    private final UserRepository userRepository;

    public void validateCreateAccountSavings(SavingsAccountDTO savingsAccountDTO) {
        if (savingsAccountRepository.existsByFullNameAndEmailAndCpf(savingsAccountDTO.fullName(), savingsAccountDTO.email(), savingsAccountDTO.cpf())) {
            throw new IllegalArgumentException("Conta já existe, não poderá ser criada");
        }

        if (savingsAccountDTO.balance().compareTo(BigDecimal.valueOf(100)) < 0) {
            throw new IllegalArgumentException("O saldo mínimo da conta deve ser de R$100,00.");
        }

        if (savingsAccountDTO.userId() == null) {
            throw new IllegalArgumentException("O usuário não pode ser nulo");
        }

        if (!userRepository.isActiveById(savingsAccountDTO.userId())) {
            throw new IllegalArgumentException("O usuario está inativo");
        }
    }

    public void validateDepositSavings(DepositRequestDTO depositRequestDTO) {
        if (!savingsAccountRepository.existsByFullNameAndEmailAndCpf(depositRequestDTO.fullName(), depositRequestDTO.email(), depositRequestDTO.cpf())) {
            throw new IllegalArgumentException("Não existe conta com este nome, email ou cpf");
        }

        SavingsAccount sender = savingsAccountRepository.findById(depositRequestDTO.accountSends())
                .orElseThrow(() -> new EntityNotFoundException("A conta de origem não existe"));
        SavingsAccount receiver = savingsAccountRepository.findById(depositRequestDTO.accountReceive())
                .orElseThrow(() -> new EntityNotFoundException("A conta de destino não existe"));

        if (Boolean.FALSE.equals(userRepository.isActiveById(sender.getUser().getId()))) {
            throw new IllegalArgumentException("O usuário que enviou o dinheiro não está ativo");
        }

        if (Boolean.FALSE.equals(userRepository.isActiveById(receiver.getUser().getId()))) {
            throw new IllegalArgumentException("O usuário que recebeu o dinheiro não está ativo");
        }

        if (depositRequestDTO.balance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do depósito deve ser positivo.");
        }
    }

    public SavingsAccount validateSavingsAccountToFalse(UUID uuid) {
        SavingsAccount savingsAccount = savingsAccountRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("A conta poupança não existe"));

        if (savingsAccount.getIsActive().equals(false)) {
            throw new IllegalArgumentException("A conta já está inativa");
        }

        return savingsAccount;
    }

    public SavingsAccount validateSavingsAccountToTrue(UUID uuid) {
        SavingsAccount savingsAccount = savingsAccountRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("A conta poupança não existe"));

        if (savingsAccount.getIsActive().equals(true)) {
            throw new IllegalArgumentException("A conta já está ativa");
        }

        return savingsAccount;
    }

    public void validateSavingsAccountDeposit(WithdrawRequestDTO withdrawRequestDTO, UUID uuid) {
        SavingsAccount sa = savingsAccountRepository.findById(withdrawRequestDTO.idAccount())
                .orElseThrow(() -> new EntityNotFoundException("A conta poupança não existe"));

        if (!withdrawRequestDTO.idAccount().equals(uuid)) {
            throw new IllegalArgumentException("O id da URL é diferente do id da requisição");
        }

        if (!savingsAccountRepository.existsByFullNameAndEmailAndCpf(sa.getFullName(), sa.getEmail(), sa.getCpf())) {
            throw new IllegalArgumentException("Não existe conta com este nome, email ou cpf");
        }

        if (Boolean.FALSE.equals(userRepository.isActiveById(sa.getUser().getId()))) {
            throw new IllegalArgumentException("A conta não está ativa");
        }

        if (withdrawRequestDTO.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do depósito deve ser positivo.");
        }
    }

    public SavingsAccount validateSavingsAccountWithdrawal(WithdrawalRequestDTO withdrawalRequestDTO) {
        SavingsAccount sa = savingsAccountRepository.findById(withdrawalRequestDTO.idAccount())
                .orElseThrow(() -> new EntityNotFoundException("A conta poupança não existe"));

        if (!savingsAccountRepository.existsByFullNameAndEmailAndCpf(sa.getFullName(), sa.getEmail(), sa.getCpf())) {
            throw new IllegalArgumentException("Não existe conta com este nome, email ou cpf");
        }

        if (Boolean.FALSE.equals(userRepository.isActiveById(sa.getUser().getId()))) {
            throw new IllegalArgumentException("A conta não está ativa");
        }

        if (withdrawalRequestDTO.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do depósito deve ser positivo.");
        }

        return sa;
    }
}
