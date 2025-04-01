package com.github.kiraruto.sistemaBancario.dto;

import java.math.BigDecimal;

public record BalanceDTO(String fullName,
                         String email,
                         String phoneNumber,
                         BigDecimal balance,
                         Boolean isActive) {
}
