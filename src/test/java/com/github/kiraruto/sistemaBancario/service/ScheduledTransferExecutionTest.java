package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.ScheduledTransfer;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumMaritalStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTypeDocument;
import com.github.kiraruto.sistemaBancario.model.enums.EnumUserRole;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.ScheduledTransferRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regressão do P0: a execução de uma transferência agendada precisa ser atômica.
 *
 * Antes da correção, o débito era salvo antes do crédito e, se o crédito falhasse,
 * o dinheiro desaparecia da conta de origem. O status da transferência também podia
 * ficar inconsistente com os saldos persistidos.
 */
@DisplayName("ScheduledTransfer - execução atômica e retry")
class ScheduledTransferExecutionTest extends AbstractIntegrationTest {

    private static final BigDecimal OVERFLOW_BALANCE = new BigDecimal("9999999999999999.99");

    @Autowired
    private ScheduledTransferService scheduledTransferService;

    @Autowired
    private SavingsAccountRepository savingsAccountRepository;

    @Autowired
    private ScheduledTransferRepository scheduledTransferRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private SavingsAccount source;
    private SavingsAccount target;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        owner = new User();
        owner.setUsername("scheduled-" + suffix);
        owner.setFirstName("Titular");
        owner.setLastName("Agendamento");
        owner.setEmail("scheduled-" + suffix + "@example.com");
        owner.setPassword("password");
        owner.setRole(EnumUserRole.CLIENTE);
        owner.setActive(true);
        owner = userRepository.save(owner);

        source = savingsAccountRepository.save(newAccount("Origem", suffix, BigDecimal.valueOf(1000)));
        target = savingsAccountRepository.save(newAccount("Destino", suffix, BigDecimal.valueOf(500)));
    }

    @Test
    @DisplayName("deve debitar, creditar, concluir e registrar a transferência agendada")
    void shouldExecuteScheduledTransferAtomically() {
        ScheduledTransfer transfer = saveTransfer(EnumStatus.PENDENTE, 0);

        scheduledTransferService.processScheduledTransfers();

        assertEquals(
                0,
                balanceOf(source.getId()).compareTo(BigDecimal.valueOf(900)),
                "A conta de origem deve ser debitada"
        );
        assertEquals(
                0,
                balanceOf(target.getId()).compareTo(BigDecimal.valueOf(600)),
                "A conta de destino deve ser creditada"
        );

        ScheduledTransfer reloaded = scheduledTransferRepository.findById(transfer.getId()).orElseThrow();
        assertEquals(EnumStatus.CONCLUIDA, reloaded.getStatus());
        assertEquals(0, reloaded.getRetryCount());

        List<Transaction> transactions = transactionsBetween(source.getId(), target.getId());
        assertEquals(1, transactions.size());
        assertEquals(EnumTransactionType.TRANSFERENCIA, transactions.get(0).getTransactionType());
        assertEquals(EnumStatus.CONCLUIDA, transactions.get(0).getStatus());
    }

    @Test
    @DisplayName("deve desfazer o débito e marcar FALHOU quando o crédito falha")
    void shouldRollbackDebitAndMarkFailedWhenCreditFails() {
        forceTargetOverflow();
        ScheduledTransfer transfer = saveTransfer(EnumStatus.PENDENTE, 0);

        scheduledTransferService.processScheduledTransfers();

        assertEquals(
                0,
                balanceOf(source.getId()).compareTo(BigDecimal.valueOf(1000)),
                "O débito não pode persistir quando o crédito falha"
        );
        assertEquals(
                0,
                balanceOf(target.getId()).compareTo(OVERFLOW_BALANCE),
                "O crédito inválido não pode persistir"
        );

        ScheduledTransfer reloaded = scheduledTransferRepository.findById(transfer.getId()).orElseThrow();
        assertEquals(EnumStatus.FALHOU, reloaded.getStatus());
        assertEquals(1, reloaded.getRetryCount());

        assertTrue(
                transactionsBetween(source.getId(), target.getId()).isEmpty(),
                "Não deve existir transação registrada quando a execução falha"
        );
    }

    @Test
    @DisplayName("retry com saldo suficiente deve concluir e registrar a transferência")
    void shouldCompleteFailedTransferOnRetry() {
        ScheduledTransfer transfer = saveTransfer(EnumStatus.FALHOU, 1);

        scheduledTransferService.retryFailedTransfer(transfer.getId());

        assertEquals(
                0,
                balanceOf(source.getId()).compareTo(BigDecimal.valueOf(900)),
                "O retry deve debitar a conta de origem"
        );
        assertEquals(
                0,
                balanceOf(target.getId()).compareTo(BigDecimal.valueOf(600)),
                "O retry deve creditar a conta de destino"
        );

        ScheduledTransfer reloaded = scheduledTransferRepository.findById(transfer.getId()).orElseThrow();
        assertEquals(EnumStatus.CONCLUIDA, reloaded.getStatus());
        assertEquals(1, transactionsBetween(source.getId(), target.getId()).size());
    }

    @Test
    @DisplayName("retry com crédito inválido deve desfazer o débito e manter FALHOU")
    void shouldRollbackRetryAndKeepFailedWhenCreditFails() {
        forceTargetOverflow();
        ScheduledTransfer transfer = saveTransfer(EnumStatus.FALHOU, 1);

        assertThrows(
                RuntimeException.class,
                () -> scheduledTransferService.retryFailedTransfer(transfer.getId())
        );

        assertEquals(
                0,
                balanceOf(source.getId()).compareTo(BigDecimal.valueOf(1000)),
                "O débito do retry não pode persistir quando o crédito falha"
        );

        ScheduledTransfer reloaded = scheduledTransferRepository.findById(transfer.getId()).orElseThrow();
        assertEquals(EnumStatus.FALHOU, reloaded.getStatus());
        assertTrue(
                transactionsBetween(source.getId(), target.getId()).isEmpty(),
                "Não deve existir transação registrada quando o retry falha"
        );
    }

    private SavingsAccount newAccount(String name, String suffix, BigDecimal balance) {
        SavingsAccount account = new SavingsAccount(
                "Conta " + name,
                LocalDate.of(1990, 1, 1),
                "Rua " + name + ", 1",
                EnumMaritalStatus.SOLTEIRO,
                randomPhone(),
                name.toLowerCase() + "-" + suffix + "@example.com",
                randomValidCpf(),
                EnumTypeDocument.RG,
                "DOC" + suffix + name.charAt(0),
                BigDecimal.valueOf(0.006743),
                balance,
                true
        );
        account.setUser(owner);
        return account;
    }

    private ScheduledTransfer saveTransfer(EnumStatus status, int retryCount) {
        ScheduledTransfer transfer = new ScheduledTransfer();
        transfer.setSourceAccount(source.getId());
        transfer.setTargetAccount(target.getId());
        transfer.setAmount(BigDecimal.valueOf(100));
        transfer.setScheduledDate(LocalDateTime.now().minusMinutes(5));
        transfer.setStatus(status);
        transfer.setRetryCount(retryCount);
        transfer.setUser(owner);
        return scheduledTransferRepository.save(transfer);
    }

    private void forceTargetOverflow() {
        SavingsAccount overflowing = savingsAccountRepository.findById(target.getId()).orElseThrow();
        overflowing.setBalance(OVERFLOW_BALANCE);
        savingsAccountRepository.saveAndFlush(overflowing);
    }

    private BigDecimal balanceOf(UUID accountId) {
        return savingsAccountRepository.findById(accountId).orElseThrow().getBalance();
    }

    private List<Transaction> transactionsBetween(UUID sourceId, UUID targetId) {
        return transactionRepository.findAll().stream()
                .filter(t -> sourceId.equals(t.getAccountSends()) && targetId.equals(t.getAccountReceive()))
                .toList();
    }

    private String randomPhone() {
        return "119" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    private String randomValidCpf() {
        int[] digits = new int[11];
        for (int i = 0; i < 9; i++) {
            digits[i] = ThreadLocalRandom.current().nextInt(10);
        }
        for (int position = 9; position < 11; position++) {
            int sum = 0;
            for (int i = 0; i < position; i++) {
                sum += digits[i] * (position + 1 - i);
            }
            int digit = (sum * 10) % 11;
            digits[position] = digit == 10 ? 0 : digit;
        }
        StringBuilder cpf = new StringBuilder();
        for (int digit : digits) {
            cpf.append(digit);
        }
        return cpf.toString();
    }
}
