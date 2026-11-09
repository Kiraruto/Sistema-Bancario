package com.github.kiraruto.sistemaBancario.dto;

import com.github.kiraruto.sistemaBancario.model.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequestDTO(@NotNull(message = "A conta de origem não pode ser nula")
                                 UUID fromId,
                                 @NotNull(message = "O tipo da conta de origem não pode ser nulo")
                                 AccountType fromType,
                                 @NotNull(message = "A conta de destino não pode ser nula")
                                 UUID toId,
                                 @NotNull(message = "O tipo da conta de destino não pode ser nulo")
                                 AccountType toType,
                                 @NotNull(message = "O valor não pode ser nulo")
                                 @Positive(message = "O valor deve ser positivo")
                                 BigDecimal amount
) {
}
