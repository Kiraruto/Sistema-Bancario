package com.github.kiraruto.sistemaBancario.dto;

import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawRequestDTO(@NotNull(message = "O id da conta não pode ser nulo")
                                 UUID idAccount,
                                 @NotNull(message = "O valor não pode ser nulo")
                                 @Positive(message = "O valor deve ser positivo")
                                 BigDecimal amount,
                                 @NotNull(message = "A origem não pode ser nula")
                                 EnumOrigin enumOrigin) {
}
