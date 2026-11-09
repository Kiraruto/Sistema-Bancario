package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.WithdrawalRequestDTO;
import com.github.kiraruto.sistemaBancario.mapper.SavingsAccountMapper;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.repository.AlertAMLRepository;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.service.AuditService;
import com.github.kiraruto.sistemaBancario.service.SavingsAccountService;
import com.github.kiraruto.sistemaBancario.utils.SavingsAccountValidate;
import com.github.kiraruto.sistemaBancario.utils.TransactionValidate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Candidato: o saque usa LocalDateTime.now() (fuso do servidor) enquanto o
 * restante da aplicacao usa America/Sao_Paulo.
 *
 * Contrato esperado: a data da transacao e a janela de 24h devem usar
 * America/Sao_Paulo. O teste força o fuso padrao para UTC para expor a divergencia.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Bug: saque de poupanca usa fuso do servidor em vez de America/Sao_Paulo")
class SavingsWithdrawalTimezoneBugTest {

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

    private TimeZone originalDefault;

    @BeforeEach
    void setUp() {
        originalDefault = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(originalDefault);
    }

    @Test
    @DisplayName("transactionDate do saque deve estar no fuso America/Sao_Paulo")
    void withdrawalTransactionShouldUseSaoPauloClock() {
        UUID accountId = UUID.randomUUID();
        SavingsAccount account = new SavingsAccount();
        account.setId(accountId);
        account.setCpf("52998224725");
        account.setBalance(new BigDecimal("1000.00"));

        when(savingsAccountValidate.validateSavingsAccountWithdrawal(any(WithdrawalRequestDTO.class)))
                .thenReturn(account);
        when(transactionRepository.sumWithdrawalsByAccountAndDateTimeRange(
                eq(accountId),
                eq(EnumTransactionType.SAQUE),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        savingsAccountService.withdrawal(
                accountId,
                new WithdrawalRequestDTO(accountId, new BigDecimal("100.00"), EnumOrigin.SAQUE)
        );

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());

        LocalDateTime expected = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        long differenceInSeconds = Math.abs(
                Duration.between(expected, captor.getValue().getTransactionDate()).getSeconds()
        );

        assertTrue(
                differenceInSeconds < 10,
                "transactionDate deveria estar em America/Sao_Paulo; diferenca de "
                        + differenceInSeconds + "s. Valor gravado: "
                        + captor.getValue().getTransactionDate()
        );
    }
}
