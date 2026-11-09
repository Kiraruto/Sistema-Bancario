package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.WithdrawRequestDTO;
import com.github.kiraruto.sistemaBancario.exceptions.DepositLimitExceededException;
import com.github.kiraruto.sistemaBancario.mapper.SavingsAccountMapper;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.repository.AlertAMLRepository;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.service.AuditService;
import com.github.kiraruto.sistemaBancario.service.SavingsAccountService;
import com.github.kiraruto.sistemaBancario.utils.SavingsAccountValidate;
import com.github.kiraruto.sistemaBancario.utils.TransactionValidate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Candidato: deposito acima de R$ 10.000 e bloqueado, mas a mensagem informa
 * limite de R$ 50.000.
 *
 * Contrato esperado: a mensagem deve refletir o limite efetivamente aplicado
 * (R$ 10.000), para nao induzir o cliente ao erro.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Bug: mensagem de limite de deposito diverge da regra")
class SavingsDepositLimitMessageBugTest {

    @Mock
    private SavingsAccountRepository savingsAccountRepository;

    @Mock
    private SavingsAccountValidate savingsAccountValidate;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionValidate transactionValidate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private AlertAMLRepository alertAMLRepository;

    @Mock
    private SavingsAccountMapper savingsAccountMapper;

    @Mock
    private AccountAccessService accountAccessService;

    @InjectMocks
    private SavingsAccountService savingsAccountService;

    @Test
    @DisplayName("deposito de R$ 10.001 deve informar limite de R$ 10.000")
    void depositAboveTenThousandShouldReportTenThousandLimit() {
        UUID accountId = UUID.randomUUID();
        SavingsAccount account = new SavingsAccount();
        account.setId(accountId);
        account.setCpf("52998224725");
        account.setBalance(new BigDecimal("1000.00"));

        when(savingsAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findLasts24Hours(eq(accountId), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            if (transaction.getId() == null) {
                transaction.setId(UUID.randomUUID());
            }
            return transaction;
        });

        WithdrawRequestDTO request = new WithdrawRequestDTO(
                accountId,
                new BigDecimal("10001.00"),
                EnumOrigin.DINHEIRO_FISICO
        );

        DepositLimitExceededException exception = assertThrows(
                DepositLimitExceededException.class,
                () -> savingsAccountService.deposit(accountId, request)
        );

        assertTrue(
                exception.getMessage().contains("10.000"),
                "Mensagem deveria citar R$ 10.000, mas foi: " + exception.getMessage()
        );
    }
}
