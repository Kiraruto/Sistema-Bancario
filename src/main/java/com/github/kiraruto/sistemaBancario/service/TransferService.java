package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.TransferRequestDTO;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.enums.AccountType;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final CheckingAccountRepository checkingAccountRepository;
    private final SavingsAccountRepository savingAccountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountAccessService accountAccessService;

    @Transactional
    public void transfer(TransferRequestDTO dto) {
        BigDecimal amount = dto.amount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da transferência deve ser positivo");
        }

        switch (dto.fromType()) {
            case CHECKING -> accountAccessService.checkCheckingAccountAccess(dto.fromId());
            case SAVING -> accountAccessService.checkSavingsAccountAccess(dto.fromId());
        }

        // Bloqueia as contas sempre na mesma ordem para evitar deadlock entre transferências opostas.
        boolean fromIsFirst = dto.fromId().compareTo(dto.toId()) <= 0;

        Object first = lockAccount(
                fromIsFirst ? dto.fromId() : dto.toId(),
                fromIsFirst ? dto.fromType() : dto.toType()
        );
        Object second = lockAccount(
                fromIsFirst ? dto.toId() : dto.fromId(),
                fromIsFirst ? dto.toType() : dto.fromType()
        );

        Object source = fromIsFirst ? first : second;
        Object target = fromIsFirst ? second : first;

        debit(source, amount);
        credit(target, amount);

        transactionRepository.save(registerTransfer(dto, amount));
    }

    private Object lockAccount(UUID id, AccountType type) {
        return switch (type) {
            case CHECKING -> checkingAccountRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new IllegalArgumentException("Conta corrente não encontrada"));
            case SAVING -> savingAccountRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new IllegalArgumentException("Conta poupança não encontrada"));
        };
    }

    private void debit(Object account, BigDecimal amount) {
        if (account instanceof CheckingAccount checkingAccount) {
            if (checkingAccount.getBalance().compareTo(amount) < 0) {
                throw new IllegalArgumentException("Saldo insuficiente na conta corrente");
            }
            checkingAccount.setBalance(checkingAccount.getBalance().subtract(amount));
            checkingAccountRepository.save(checkingAccount);
        } else if (account instanceof SavingsAccount savingsAccount) {
            if (savingsAccount.getBalance().compareTo(amount) < 0) {
                throw new IllegalArgumentException("Saldo insuficiente na conta poupança");
            }
            savingsAccount.setBalance(savingsAccount.getBalance().subtract(amount));
            savingAccountRepository.save(savingsAccount);
        }
    }

    private void credit(Object account, BigDecimal amount) {
        if (account instanceof CheckingAccount checkingAccount) {
            checkingAccount.setBalance(checkingAccount.getBalance().add(amount));
            checkingAccountRepository.save(checkingAccount);
        } else if (account instanceof SavingsAccount savingsAccount) {
            savingsAccount.setBalance(savingsAccount.getBalance().add(amount));
            savingAccountRepository.save(savingsAccount);
        }
    }

    private Transaction registerTransfer(TransferRequestDTO dto, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setAccountSends(dto.fromId());
        transaction.setAccountReceive(dto.toId());
        transaction.setTransactionType(EnumTransactionType.TRANSFERENCIA);
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
        transaction.setStatus(EnumStatus.CONCLUIDA);
        transaction.setOrigin(EnumOrigin.TED);
        transaction.setDescription("Transferência entre contas");
        return transaction;
    }
}
