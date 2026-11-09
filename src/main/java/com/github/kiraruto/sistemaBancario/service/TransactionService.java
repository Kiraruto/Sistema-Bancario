package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.exceptions.PendingTransactionNotFoundException;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRespository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final CheckingAccountRepository checkingAccountRepository;

    @Transactional
    public void completed(UUID transactionId) {
        Transaction transaction = transactionRespository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("O Id não existe"));

        if (transaction.getStatus() != EnumStatus.PENDENTE) {
            throw new IllegalStateException("Somente transações pendentes podem ser concluídas.");
        }

        if (transaction.getTransactionType() != EnumTransactionType.DEPOSITO) {
            throw new IllegalStateException("Somente depósitos pendentes podem ser concluídos.");
        }

        creditDestinationAccount(transaction.getAccountReceive(), transaction.getAmount());

        transaction.setStatus(EnumStatus.CONCLUIDA);
        transaction.setDescription("Depósito");
        transactionRespository.save(transaction);
    }

    @Transactional
    public void failure(UUID transactionId) {
        Transaction transaction = transactionRespository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("O Id não existe"));

        if (transaction.getStatus() != EnumStatus.PENDENTE) {
            throw new IllegalStateException("Somente transações pendentes podem ser marcadas como falha.");
        }

        transaction.setStatus(EnumStatus.FALHOU);
        transaction.setDescription("Depósito falhou");
        transactionRespository.save(transaction);
    }

    public List<Transaction> getAllTransactionPendente(UUID uuid) {
        List<Transaction> transactions = transactionRespository.findAllByIdTypePendente(uuid, EnumStatus.PENDENTE);

        if (transactions.isEmpty()) {
            throw new PendingTransactionNotFoundException("Nenhuma transação pendente encontrada.");
        }

        return transactions;
    }

    private void creditDestinationAccount(UUID accountId, BigDecimal amount) {
        Optional<SavingsAccount> savingsAccount = savingsAccountRepository.findByIdForUpdate(accountId);

        if (savingsAccount.isPresent()) {
            SavingsAccount account = savingsAccount.get();
            account.setBalance(account.getBalance().add(amount));
            savingsAccountRepository.save(account);
            return;
        }

        CheckingAccount checkingAccount = checkingAccountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Conta de destino não encontrada: " + accountId));

        checkingAccount.setBalance(checkingAccount.getBalance().add(amount));
        checkingAccountRepository.save(checkingAccount);
    }
}
