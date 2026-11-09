package com.github.kiraruto.sistemaBancario.utils;

import com.github.kiraruto.sistemaBancario.dto.WithdrawRequestDTO;
import com.github.kiraruto.sistemaBancario.dto.WithdrawalRequestDTO;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.utils.CheckingAccountValidate;
import com.github.kiraruto.sistemaBancario.utils.SavingsAccountValidate;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Candidato: validadores chamam Optional.get() sem isPresent().
 *
 * Conta inexistente deve virar erro de negocio (EntityNotFoundException -> 404),
 * nao NoSuchElementException (-> 500).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Bug: Optional.get() sem isPresent em validadores de conta")
class MissingAccountOptionalBugTest {

    @Mock
    private SavingsAccountRepository savingsAccountRepository;

    @Mock
    private CheckingAccountRepository checkingAccountRepository;

    @Mock
    private UserRepository userRepository;

    private SavingsAccountValidate savingsAccountValidate;
    private CheckingAccountValidate checkingAccountValidate;

    @BeforeEach
    void setUp() {
        savingsAccountValidate = new SavingsAccountValidate(savingsAccountRepository, userRepository);
        checkingAccountValidate = new CheckingAccountValidate(checkingAccountRepository, userRepository);
    }

    @Test
    @DisplayName("deposito em poupanca inexistente deve ser 404, nao 500")
    void savingsDepositOnMissingAccountShouldBeNotFound() {
        UUID accountId = UUID.randomUUID();
        when(savingsAccountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                savingsAccountValidate.validateSavingsAccountDeposit(
                        new WithdrawRequestDTO(accountId, BigDecimal.TEN, EnumOrigin.DINHEIRO_FISICO),
                        accountId
                ));
    }

    @Test
    @DisplayName("saque em poupanca inexistente deve ser 404, nao 500")
    void savingsWithdrawalOnMissingAccountShouldBeNotFound() {
        UUID accountId = UUID.randomUUID();
        when(savingsAccountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                savingsAccountValidate.validateSavingsAccountWithdrawal(
                        new WithdrawalRequestDTO(accountId, BigDecimal.TEN, EnumOrigin.SAQUE)
                ));
    }

    @Test
    @DisplayName("deposito em conta corrente inexistente deve ser 404, nao 500")
    void checkingDepositOnMissingAccountShouldBeNotFound() {
        UUID accountId = UUID.randomUUID();
        when(checkingAccountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                checkingAccountValidate.validateCheckingAccountWithdraw(
                        new WithdrawRequestDTO(accountId, BigDecimal.TEN, EnumOrigin.DINHEIRO_FISICO)
                ));
    }

    @Test
    @DisplayName("saque em conta corrente inexistente deve ser 404, nao 500")
    void checkingWithdrawalOnMissingAccountShouldBeNotFound() {
        UUID accountId = UUID.randomUUID();
        when(checkingAccountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                checkingAccountValidate.validateCheckingAccountWithdrawal(
                        new WithdrawalRequestDTO(accountId, BigDecimal.TEN, EnumOrigin.SAQUE)
                ));
    }
}
