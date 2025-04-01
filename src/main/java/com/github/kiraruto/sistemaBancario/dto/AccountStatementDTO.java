package com.github.kiraruto.sistemaBancario.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountStatementDTO(UUID id,
                                  String name,
                                  BigDecimal balance,
                                  TransactionDTO transaction) {
}
