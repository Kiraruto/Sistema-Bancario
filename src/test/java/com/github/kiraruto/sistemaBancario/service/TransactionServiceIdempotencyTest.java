package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.SavingsAccountDTO;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumMaritalStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTypeDocument;
import com.github.kiraruto.sistemaBancario.model.enums.EnumUserRole;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regressão do P0: concluir a mesma transação duas vezes não pode creditar duas vezes.
 */
@Transactional
@DisplayName("TransactionService - idempotência de conclusão")
class TransactionServiceIdempotencyTest extends AbstractIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SavingsAccountRepository savingsAccountRepository;

    @Autowired
    private UserRepository userRepository;

    private SavingsAccount account;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("idem-" + System.nanoTime());
        user.setFirstName("Teste");
        user.setLastName("Idempotencia");
        user.setEmail("idem-" + System.nanoTime() + "@example.com");
        user.setPassword("password");
        user.setRole(EnumUserRole.CLIENTE);
        user.setActive(true);
        user = userRepository.save(user);

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        SavingsAccountDTO dto = new SavingsAccountDTO(
                "Titular Idempotencia",
                LocalDate.of(1990, 1, 1),
                "Rua Teste, 1",
                EnumMaritalStatus.SOLTEIRO,
                "119" + suffix.substring(0, 8),
                "conta-" + suffix + "@example.com",
                "52998224725",
                EnumTypeDocument.RG,
                "DOC" + suffix,
                BigDecimal.valueOf(100),
                user.getId()
        );

        account = new SavingsAccount(dto);
        account.setUser(user);
        account.setBalance(BigDecimal.valueOf(100));
        account = savingsAccountRepository.save(account);
    }

    @Test
    @DisplayName("não deve creditar duas vezes ao concluir a mesma transação")
    void shouldNotCreditTwiceWhenCompletedIsCalledTwice() {
        Transaction transaction = pendingDeposit(BigDecimal.valueOf(50));
        transaction = transactionRepository.save(transaction);
        UUID transactionId = transaction.getId();

        transactionService.completed(transactionId);

        assertEquals(
                BigDecimal.valueOf(150),
                savingsAccountRepository.findById(account.getId()).orElseThrow().getBalance()
        );

        Transaction pending = transactionRepository.findById(transactionId).orElseThrow();
        assertEquals(EnumStatus.CONCLUIDA, pending.getStatus());

        assertThrows(
                IllegalStateException.class,
                () -> transactionService.completed(transactionId)
        );

        assertEquals(
                BigDecimal.valueOf(150),
                savingsAccountRepository.findById(account.getId()).orElseThrow().getBalance(),
                "A segunda chamada não pode creditar novamente"
        );
    }

    @Test
    @DisplayName("não deve concluir saque pendente como se fosse depósito")
    void shouldNotCompletePendingWithdrawalAsDeposit() {
        Transaction withdrawal = new Transaction();
        withdrawal.setAccountSends(account.getId());
        withdrawal.setAccountReceive(account.getId());
        withdrawal.setTransactionType(EnumTransactionType.SAQUE);
        withdrawal.setAmount(BigDecimal.valueOf(30));
        withdrawal.setTransactionDate(LocalDateTime.now());
        withdrawal.setStatus(EnumStatus.PENDENTE);
        withdrawal = transactionRepository.save(withdrawal);

        Transaction finalWithdrawal = withdrawal;
        assertThrows(
                IllegalStateException.class,
                () -> transactionService.completed(finalWithdrawal.getId())
        );

        assertEquals(
                BigDecimal.valueOf(100),
                savingsAccountRepository.findById(account.getId()).orElseThrow().getBalance(),
                "Saque pendente não pode creditar a conta"
        );
    }

    private Transaction pendingDeposit(BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setAccountSends(account.getId());
        transaction.setAccountReceive(account.getId());
        transaction.setTransactionType(EnumTransactionType.DEPOSITO);
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(EnumStatus.PENDENTE);
        return transaction;
    }
}
