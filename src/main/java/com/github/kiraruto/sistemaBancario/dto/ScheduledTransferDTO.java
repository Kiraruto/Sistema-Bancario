package com.github.kiraruto.sistemaBancario.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ScheduledTransferDTO(
        @NotNull(message = "Conta de origem é obrigatória")
        UUID accountSends,

        @NotNull(message = "Conta de destino é obrigatória")
        UUID accountReceive,

        @Positive(message = "Valor deve ser positivo")
        BigDecimal amount,

        @Future(message = "Data deve ser futura")
        LocalDateTime scheduledDate,

        @NotNull(message = "O id do usuario não pode ser nulo")
        @JsonAlias("userId")
        String userId) {
}