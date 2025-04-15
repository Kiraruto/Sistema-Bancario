package com.github.kiraruto.sistemaBancario.exceptions;

public class DepositLimitExceededException extends RuntimeException {
    public DepositLimitExceededException(String message) {
        super(message);
    }
}
