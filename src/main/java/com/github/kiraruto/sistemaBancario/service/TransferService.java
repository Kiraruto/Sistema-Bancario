package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.TransferRequestDTO;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.enums.AccountType;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        UUID firstLockId = dto.fromId().compareTo(dto.toId()) < 0 ? dto.fromId() : dto.toId();
        UUID secondLockId = dto.fromId().compareTo(dto.toId()) < 0 ? dto.toId() : dto.fromId();

        ReentrantLock firstLock = getLock(firstLockId);
        ReentrantLock secondLock = getLock(secondLockId);

        firstLock.lock();
        try {
            secondLock.lock();
            try {
                Object fromAccount = getAccount(dto.fromId(), dto.fromType());
                Object toAccount = getAccount(dto.toId(), dto.toType());

                if (fromAccount instanceof CheckingAccount c) {
                    if (c.getBalance().compareTo(dto.amount()) < 0) {
                        throw new IllegalArgumentException("Saldo insuficiente na conta de origem (corrente)");
                    }
                    c.setBalance(c.getBalance().subtract(dto.amount()));
                    checkingAccountRepository.save(c);
                } else if (fromAccount instanceof SavingsAccount s) {
                    if (s.getBalance().compareTo(dto.amount()) < 0) {
                        throw new IllegalArgumentException("Saldo insuficiente na conta de origem (poupança)");
                    }
                    s.setBalance(s.getBalance().subtract(dto.amount()));
                    savingAccountRepository.save(s);
                } else {
                    throw new IllegalArgumentException("Tipo de conta de origem inválido");
                }

                if (toAccount instanceof CheckingAccount c) {
                    c.setBalance(c.getBalance().add(dto.amount()));
                    checkingAccountRepository.save(c);
                } else if (toAccount instanceof SavingsAccount s) {
                    s.setBalance(s.getBalance().add(dto.amount()));
                    savingAccountRepository.save(s);
                } else {
                    throw new IllegalArgumentException("Tipo de conta de destino inválido");
                }

            } finally {
                secondLock.unlock();
            }
        } finally {
            firstLock.unlock();
        }
    }

    private Object getAccount(UUID id, AccountType type) {
        return switch (type) {
            case CHECKING -> checkingAccountRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Conta corrente não encontrada"));
            case SAVING -> savingAccountRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Conta poupança não encontrada"));
        };
    }
}
