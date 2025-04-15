package com.github.kiraruto.sistemaBancario.exceptions;

public class FrequentLargeDepositsException extends RuntimeException {
    public FrequentLargeDepositsException(String message) {
        super(message);
    }
}
