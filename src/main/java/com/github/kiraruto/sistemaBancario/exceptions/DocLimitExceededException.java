package com.github.kiraruto.sistemaBancario.exceptions;

public class DocLimitExceededException extends RuntimeException {
    public DocLimitExceededException(String message) {
        super(message);
    }
}
