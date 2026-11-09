package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Service Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SavingsAccountRepository savingsAccountRepository;

    @Mock
    private CheckingAccountRepository checkingAccountRepository;

    @InjectMocks
    private TransactionService transactionService;

    private UUID transactionId;
    private UUID receiveAccountId;
    private Transaction pendingTransaction;
    private Transaction completedTransaction;
    private Transaction failedTransaction;
    private SavingsAccount receiveAccount;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        receiveAccountId = UUID.randomUUID();

        receiveAccount = new SavingsAccount();
        receiveAccount.setId(receiveAccountId);
        receiveAccount.setBalance(BigDecimal.valueOf(100));

        pendingTransaction = new Transaction();
        pendingTransaction.setId(transactionId);
        pendingTransaction.setStatus(EnumStatus.PENDENTE);
        pendingTransaction.setTransactionType(EnumTransactionType.DEPOSITO);
        pendingTransaction.setAccountReceive(receiveAccountId);
        pendingTransaction.setAmount(BigDecimal.valueOf(10));

        completedTransaction = new Transaction();
        completedTransaction.setId(transactionId);
        completedTransaction.setStatus(EnumStatus.CONCLUIDA);
        completedTransaction.setTransactionType(EnumTransactionType.DEPOSITO);

        failedTransaction = new Transaction();
        failedTransaction.setId(transactionId);
        failedTransaction.setStatus(EnumStatus.FALHOU);
        failedTransaction.setTransactionType(EnumTransactionType.DEPOSITO);
    }

    @Nested
    @DisplayName("completed() Tests")
    class CompleteTransactionTests {

        @Test
        @DisplayName("Should change status to COMPLETED and credit the account when transaction is pending")
        void shouldChangeStatusToCompleted() {
            when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(pendingTransaction));
            when(savingsAccountRepository.findByIdForUpdate(receiveAccountId)).thenReturn(Optional.of(receiveAccount));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            transactionService.completed(transactionId);

            verify(transactionRepository).save(pendingTransaction);
            assertEquals(EnumStatus.CONCLUIDA, pendingTransaction.getStatus());
            assertEquals(BigDecimal.valueOf(110), receiveAccount.getBalance());
        }

        @Test
        @DisplayName("Should throw exception when transaction ID doesn't exist")
        void shouldThrowWhenTransactionNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(transactionRepository.findByIdForUpdate(nonExistentId)).thenReturn(Optional.empty());

            Exception exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.completed(nonExistentId)
            );

            assertEquals("O Id não existe", exception.getMessage());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should refuse to complete an already completed transaction")
        void shouldRefuseToCompleteAnAlreadyCompletedTransaction() {
            when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(completedTransaction));

            assertThrows(
                    IllegalStateException.class,
                    () -> transactionService.completed(transactionId)
            );

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should refuse to complete a pending withdrawal")
        void shouldRefuseToCompleteAPendingWithdrawal() {
            pendingTransaction.setTransactionType(EnumTransactionType.SAQUE);
            when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(pendingTransaction));

            assertThrows(
                    IllegalStateException.class,
                    () -> transactionService.completed(transactionId)
            );

            verify(transactionRepository, never()).save(any());
            verify(savingsAccountRepository, never()).findByIdForUpdate(any());
        }

        @Test
        @DisplayName("Should throw exception when repository save fails")
        void shouldThrowWhenSaveFails() {
            when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(pendingTransaction));
            when(savingsAccountRepository.findByIdForUpdate(receiveAccountId)).thenReturn(Optional.of(receiveAccount));
            when(transactionRepository.save(any(Transaction.class))).thenThrow(new RuntimeException("Database error"));

            assertThrows(RuntimeException.class,
                    () -> transactionService.completed(transactionId));
        }
    }

    @Nested
    @DisplayName("failure() Tests")
    class FailTransactionTests {

        @Test
        @DisplayName("Should change status to FAILED when transaction is pending")
        void shouldChangeStatusToFailed() {
            when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(pendingTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            transactionService.failure(transactionId);

            verify(transactionRepository).save(pendingTransaction);
            assertEquals(EnumStatus.FALHOU, pendingTransaction.getStatus());
        }

        @Test
        @DisplayName("Should throw exception when transaction ID doesn't exist")
        void shouldThrowWhenTransactionNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(transactionRepository.findByIdForUpdate(nonExistentId)).thenReturn(Optional.empty());

            Exception exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.failure(nonExistentId)
            );

            assertEquals("O Id não existe", exception.getMessage());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should refuse to fail an already failed transaction")
        void shouldRefuseToFailAnAlreadyFailedTransaction() {
            when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(failedTransaction));

            assertThrows(
                    IllegalStateException.class,
                    () -> transactionService.failure(transactionId)
            );

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should refuse to fail an already completed transaction")
        void shouldRefuseToFailAnAlreadyCompletedTransaction() {
            when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(completedTransaction));

            assertThrows(
                    IllegalStateException.class,
                    () -> transactionService.failure(transactionId)
            );

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should verify correct repository call sequence")
        void shouldVerifyRepositoryCallSequence() {
            when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(pendingTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            transactionService.failure(transactionId);

            InOrder inOrder = inOrder(transactionRepository);
            inOrder.verify(transactionRepository).findByIdForUpdate(transactionId);
            inOrder.verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Should throw exception when repository save fails")
        void shouldThrowWhenSaveFails() {
            when(transactionRepository.findByIdForUpdate(transactionId)).thenReturn(Optional.of(pendingTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenThrow(new RuntimeException("Database error"));

            assertThrows(RuntimeException.class,
                    () -> transactionService.failure(transactionId));
        }
    }
}
