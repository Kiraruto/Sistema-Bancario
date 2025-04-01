package com.github.kiraruto.sistemaBancario.dto;

import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositRequestDTO(@NotBlank(message = "O nome completo não pode ser em branco")
                                String fullName,
                                @NotBlank(message = "O email não pode ser nulo")
                                String email,
                                @NotNull(message = "O cpf não pode ser nulo")
                                String cpf,
                                @NotNull(message = "O saldo da conta não pode ser nulo")
                                BigDecimal balance,
                                @NotNull(message = "A origem não pode ser nula")
                                EnumOrigin origin,
                                @NotBlank(message = "O id da conta que manda o dinheiro não pode estar vazio")
                                UUID accountSends,
                                @NotBlank(message = "O id da conta que recebe o dinheiro não pode estar vazio")
                                UUID accountReceive,
                                String description,
                                @NotNull(message = "O id do usuario não pode ser nulo")
                                UUID userId) {
}
