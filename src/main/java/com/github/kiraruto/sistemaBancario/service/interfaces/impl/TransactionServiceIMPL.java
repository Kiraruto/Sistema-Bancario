package com.github.kiraruto.sistemaBancario.service.interfaces.impl;

import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.service.interfaces.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceIMPL implements TransactionService {
    private final TransactionRepository transactionRespository;

    public void completed(UUID transactionId) {
        if (!transactionRespository.existsById(transactionId)) {
            throw new IllegalArgumentException("O Id não existe");
        }

        var transaction = transactionRespository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("O Id não existe"));

        transaction.setStatus(EnumStatus.CONCLUIDA);
        transactionRespository.save(transaction);
    }

    public void failure(UUID transactionId) {
        if (!transactionRespository.existsById(transactionId)) {
            throw new IllegalArgumentException("O Id não existe");
        }

        var transaction = transactionRespository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("O Id não existe"));

        transaction.setStatus(EnumStatus.FALHOU);
        transactionRespository.save(transaction);
    }
}
