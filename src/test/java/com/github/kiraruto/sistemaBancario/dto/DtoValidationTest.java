package com.github.kiraruto.sistemaBancario.dto;

import com.github.kiraruto.sistemaBancario.model.enums.AccountType;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regressão do P1: os DTOs de movimentação precisam rejeitar campos ausentes e
 * valores inválidos antes de chegarem à regra de negócio.
 */
@DisplayName("DTOs - validação de entrada")
class DtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("transferência válida não deve ter violações")
    void validTransferShouldPass() {
        TransferRequestDTO dto = new TransferRequestDTO(
                UUID.randomUUID(), AccountType.CHECKING,
                UUID.randomUUID(), AccountType.SAVING,
                BigDecimal.TEN
        );

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    @DisplayName("transferência sem contas, tipos ou valor deve ser rejeitada")
    void invalidTransferShouldFail() {
        TransferRequestDTO dto = new TransferRequestDTO(null, null, null, null, null);

        Set<String> fields = violatedFields(validator.validate(dto));

        assertEquals(Set.of("fromId", "fromType", "toId", "toType", "amount"), fields);
    }

    @Test
    @DisplayName("transferência com valor negativo ou zero deve ser rejeitada")
    void nonPositiveTransferAmountShouldFail() {
        TransferRequestDTO negative = new TransferRequestDTO(
                UUID.randomUUID(), AccountType.CHECKING,
                UUID.randomUUID(), AccountType.SAVING,
                BigDecimal.valueOf(-1)
        );

        assertEquals(Set.of("amount"), violatedFields(validator.validate(negative)));
    }

    @Test
    @DisplayName("depósito sem conta, valor ou origem deve ser rejeitado")
    void invalidWithdrawRequestShouldFail() {
        WithdrawRequestDTO dto = new WithdrawRequestDTO(null, null, null);

        assertEquals(
                Set.of("idAccount", "amount", "enumOrigin"),
                violatedFields(validator.validate(dto))
        );
    }

    @Test
    @DisplayName("saque sem conta, valor ou origem deve ser rejeitado")
    void invalidWithdrawalRequestShouldFail() {
        WithdrawalRequestDTO dto = new WithdrawalRequestDTO(null, BigDecimal.ZERO, null);

        assertEquals(
                Set.of("idAccount", "amount", "enumOrigin"),
                violatedFields(validator.validate(dto))
        );
    }

    @Test
    @DisplayName("agendamento sem valor ou data deve ser rejeitado")
    void invalidScheduledTransferShouldFail() {
        ScheduledTransferDTO dto = new ScheduledTransferDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                UUID.randomUUID().toString()
        );

        assertEquals(
                Set.of("amount", "scheduledDate"),
                violatedFields(validator.validate(dto))
        );
    }

    @Test
    @DisplayName("agendamento válido não deve ter violações")
    void validScheduledTransferShouldPass() {
        ScheduledTransferDTO dto = new ScheduledTransferDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.TEN,
                LocalDateTime.now().plusDays(1),
                UUID.randomUUID().toString()
        );

        assertTrue(validator.validate(dto).isEmpty());
    }

    private Set<String> violatedFields(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
