package com.github.kiraruto.sistemaBancario.service.interfaces;


import java.util.UUID;

public interface TransactionService {

    void completed(UUID transactionId);

    void failure(UUID transactionId);
}
