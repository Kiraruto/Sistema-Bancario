package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.exceptions.PendingTransactionNotFoundException;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRespository;
    private final SavingsAccountRepository savingsAccountRepository;

    private final ConcurrentHashMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Transactional
    public void completed(UUID transactionId) {
        ReentrantLock lock = locks.computeIfAbsent(transactionId, k -> new ReentrantLock());

        lock.lock();
        try {
            var transaction = transactionRespository.findById(transactionId)
                    .orElseThrow(() -> new IllegalArgumentException("O Id não existe"));

            SavingsAccount sa = savingsAccountRepository.findById(transaction.getAccountReceive())
                    .orElseThrow(() -> new IllegalArgumentException("Conta de destino não encontrada: " + transaction.getAccountReceive()));

            BigDecimal newBalance = sa.getBalance().add(transaction.getAmount());
            sa.setBalance(newBalance);

            savingsAccountRepository.save(sa);

            transaction.setStatus(EnumStatus.CONCLUIDA);
            transaction.setDescription("Depósito");

            transactionRespository.save(transaction);
        } finally {
            lock.unlock();
            locks.remove(transactionId, lock);
        }
    }

    public void failure(UUID transactionId) {
        ReentrantLock lock = locks.computeIfAbsent(transactionId, k -> new ReentrantLock());

        lock.lock();
        try {
            var transaction = transactionRespository.findById(transactionId)
                    .orElseThrow(() -> new IllegalArgumentException("O Id não existe"));

            transaction.setStatus(EnumStatus.FALHOU);
            transaction.setDescription("Depósito falhou");
            transactionRespository.save(transaction);
        } finally {
            lock.unlock();
            locks.remove(transactionId, lock);
        }
    }


    public List<Transaction> getAllTransactionPendente(UUID uuid) {
        List<Transaction> transactions = transactionRespository.findAllByIdTypePendente(uuid, EnumStatus.PENDENTE);

        if (transactions.isEmpty()) {
            throw new PendingTransactionNotFoundException("Nenhuma transação pendente encontrada.");
        }

        return transactions;
    }
}
