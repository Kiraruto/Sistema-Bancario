package com.github.kiraruto.sistemaBancario.exceptions;

public class InvalidAccountIdException extends RuntimeException {
    public InvalidAccountIdException(String message) {
        super(message);
    }
}
