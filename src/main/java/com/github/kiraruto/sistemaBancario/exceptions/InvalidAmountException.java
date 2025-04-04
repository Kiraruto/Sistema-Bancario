package com.github.kiraruto.sistemaBancario.exceptions;

public class InvalidAmountException extends RuntimeException {
  public InvalidAmountException(String message) {
    super(message);
  }
}
