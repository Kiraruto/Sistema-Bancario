package com.github.kiraruto.sistemaBancario.dto;

import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record TransactionDTO(
        @NotNull(message = "A conta de envio não pode ser nula")
        UUID accountSends,

        @NotNull(message = "A conta de recebimento não pode ser nula")
        UUID accountReceive,

        @NotNull(message = "O tipo de transação não pode ser nulo")
        EnumTransactionType transactionType,

        @NotNull(message = "O valor da transação não pode ser nulo")
        @Positive(message = "O valor da transação deve ser positivo")
        BigDecimal amount,

        @NotNull(message = "A data da transação não pode ser nula")
        LocalDateTime transactionDate,

        @NotNull(message = "A origem da transação não pode ser nula")
        EnumOrigin origin,

        @NotNull(message = "O status da transação não pode ser nulo")
        EnumStatus status,

        String description
) {
    public TransactionDTO(Transaction t) {
        this(t.getAccountSends(), t.getAccountReceive(), t.getTransactionType(), t.getAmount(), t.getTransactionDate(), t.getOrigin(), t.getStatus(), t.getDescription());
    }

    public static List<TransactionDTO> TransformInListTransicion(List<Transaction> allById) {
        List<TransactionDTO> dtoList = new ArrayList<>();

        for (Transaction transaction : allById) {
            dtoList.add(new TransactionDTO(transaction));
        }

        return dtoList;
    }
}
