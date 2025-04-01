package com.github.kiraruto.sistemaBancario.exceptions;

public class InvalidDepositOriginException extends RuntimeException {
    public InvalidDepositOriginException(String message) {
        super(message);
    }
}
