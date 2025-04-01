package com.github.kiraruto.sistemaBancario.exceptions;

public class RegisterDuplicateException extends RuntimeException {
    public RegisterDuplicateException(String message) {
        super(message);
    }
}
