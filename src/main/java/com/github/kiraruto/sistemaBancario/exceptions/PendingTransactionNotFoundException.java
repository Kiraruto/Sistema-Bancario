package com.github.kiraruto.sistemaBancario.exceptions;

public class PendingTransactionNotFoundException extends RuntimeException {
    public PendingTransactionNotFoundException(String message) {
        super(message);
    }
}
