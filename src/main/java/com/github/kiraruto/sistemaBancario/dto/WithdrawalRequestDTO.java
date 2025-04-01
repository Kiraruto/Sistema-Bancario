package com.github.kiraruto.sistemaBancario.dto;

import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawalRequestDTO(UUID idAccount,
                                   BigDecimal amount,
                                   EnumOrigin enumOrigin) {
}
