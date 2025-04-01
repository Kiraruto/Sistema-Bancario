package com.github.kiraruto.sistemaBancario.exceptions;

public class FraudDetectedException extends RuntimeException {
    public FraudDetectedException(String message) {
        super(message);
    }
}
