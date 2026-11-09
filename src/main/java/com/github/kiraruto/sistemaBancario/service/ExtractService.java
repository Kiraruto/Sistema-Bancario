package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.exceptions.TransictionsEmpty;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExtractService {

    private final TransactionRepository transactionRespository;

    public List<Transaction> getallById(UUID id) {
        List<Transaction> transactions = transactionRespository.findAllByAccount(id);

        if (transactions.isEmpty()) {
            throw new TransictionsEmpty("Nenhuma transação encontrada.");
        }

        return transactions;
    }
}
