package com.github.kiraruto.sistemaBancario.utils;

import com.github.kiraruto.sistemaBancario.dto.DepositRequestDTO;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.service.AuditService;
import com.github.kiraruto.sistemaBancario.utils.DepositValidator;
import com.github.kiraruto.sistemaBancario.utils.TransactionValidate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Candidato: a checagem de conta ativa esta invertida.
 *
 * Hoje: conta ATIVA retorna FALHOU e conta INATIVA segue como CONCLUIDA.
 * Contrato esperado: conta ativa conclui; conta inativa falha.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Bug: checagem de conta ativa invertida em TransactionValidate")
class TransactionValidateActiveAccountBugTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SavingsAccountRepository savingsAccountRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private DepositValidator depositValidator;

    @InjectMocks
    private TransactionValidate transactionValidate;

    @Test
    @DisplayName("conta de destino ativa deve concluir o deposito")
    void activeAccountShouldCompleteDeposit() {
        UUID accountId = UUID.randomUUID();
        SavingsAccount account = new SavingsAccount();
        account.setId(accountId);
        account.setIsActive(true);

        when(savingsAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findLast24HoursByAccountSends(accountId)).thenReturn(List.of());

        assertEquals(EnumStatus.CONCLUIDA, transactionValidate.deposit(requestFor(accountId)));
    }

    @Test
    @DisplayName("conta de destino inativa deve falhar o deposito")
    void inactiveAccountShouldFailDeposit() {
        UUID accountId = UUID.randomUUID();
        SavingsAccount account = new SavingsAccount();
        account.setId(accountId);
        account.setIsActive(false);

        when(savingsAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findLast24HoursByAccountSends(accountId)).thenReturn(List.of());

        assertEquals(EnumStatus.FALHOU, transactionValidate.deposit(requestFor(accountId)));
    }

    private DepositRequestDTO requestFor(UUID accountId) {
        return new DepositRequestDTO(
                "Fulano de Tal",
                "fulano@example.com",
                "52998224725",
                new BigDecimal("100.00"),
                EnumOrigin.DINHEIRO_FISICO,
                UUID.randomUUID(),
                accountId,
                "Deposito",
                UUID.randomUUID()
        );
    }
}
