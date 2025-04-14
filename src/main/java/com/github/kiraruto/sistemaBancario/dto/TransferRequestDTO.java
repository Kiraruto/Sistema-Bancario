package com.github.kiraruto.sistemaBancario.dto;

import com.github.kiraruto.sistemaBancario.model.enums.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequestDTO(
        UUID fromId,
        AccountType fromType,
        UUID toId,
        AccountType toType,
        BigDecimal amount
) {
}
