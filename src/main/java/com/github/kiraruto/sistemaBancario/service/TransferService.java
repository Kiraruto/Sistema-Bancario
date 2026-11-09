package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.TransferRequestDTO;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.enums.AccountType;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final CheckingAccountRepository checkingAccountRepository;
    private final SavingsAccountRepository savingAccountRepository;
    private final ConcurrentHashMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    private ReentrantLock getLock(UUID id) {
        return locks.computeIfAbsent(id, k -> new ReentrantLock());
    }

    public void transfer(TransferRequestDTO dto) {
        if (dto.amount() == null || dto.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da transferência deve ser positivo");
        }

        UUID firstLockId = dto.fromId().compareTo(dto.toId()) < 0 ? dto.fromId() : dto.toId();
        UUID secondLockId = dto.fromId().compareTo(dto.toId()) < 0 ? dto.toId() : dto.fromId();

        ReentrantLock firstLock = getLock(firstLockId);
        ReentrantLock secondLock = getLock(secondLockId);

        firstLock.lock();
        try {
            secondLock.lock();
            try {
                debit(dto.fromId(), dto.fromType(), dto.amount());
                credit(dto.toId(), dto.toType(), dto.amount());
            } finally {
                secondLock.unlock();
            }
        } finally {
            firstLock.unlock();
        }
    }

    private void debit(UUID id, AccountType type, BigDecimal amount) {
        switch (type) {
            case CHECKING -> {
                CheckingAccount account = checkingAccountRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Conta corrente não encontrada"));
                if (account.getBalance().compareTo(amount) < 0)
                    throw new IllegalArgumentException("Saldo insuficiente na conta corrente");
                account.setBalance(account.getBalance().subtract(amount));
                checkingAccountRepository.save(account);
            }
            case SAVING -> {
                SavingsAccount account = savingAccountRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Conta poupança não encontrada"));
                if (account.getBalance().compareTo(amount) < 0)
                    throw new IllegalArgumentException("Saldo insuficiente na conta poupança");
                account.setBalance(account.getBalance().subtract(amount));
                savingAccountRepository.save(account);
            }
        }
    }

    private void credit(UUID id, AccountType type, BigDecimal amount) {
        switch (type) {
            case CHECKING -> {
                CheckingAccount account = checkingAccountRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Conta corrente não encontrada"));
                account.setBalance(account.getBalance().add(amount));
                checkingAccountRepository.save(account);
            }
            case SAVING -> {
                SavingsAccount account = savingAccountRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Conta poupança não encontrada"));
                account.setBalance(account.getBalance().add(amount));
                savingAccountRepository.save(account);
            }
        }
    }
}
