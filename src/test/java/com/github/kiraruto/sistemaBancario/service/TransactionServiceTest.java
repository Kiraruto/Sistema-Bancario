package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Service Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private UUID transactionId;
    private Transaction pendingTransaction;
    private Transaction completedTransaction;
    private Transaction failedTransaction;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();

        pendingTransaction = new Transaction();
        pendingTransaction.setId(transactionId);
        pendingTransaction.setStatus(EnumStatus.PENDENTE);

        completedTransaction = new Transaction();
        completedTransaction.setId(transactionId);
        completedTransaction.setStatus(EnumStatus.CONCLUIDA);

        failedTransaction = new Transaction();
        failedTransaction.setId(transactionId);
        failedTransaction.setStatus(EnumStatus.FALHOU);
    }

    @Nested
    @DisplayName("completeTransaction() Tests")
    class CompleteTransactionTests {

        @Test
        @DisplayName("Should change status to COMPLETED when transaction exists")
        void shouldChangeStatusToCompleted() {
            when(transactionRepository.existsById(transactionId)).thenReturn(true);
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(pendingTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            transactionService.completed(transactionId);

            verify(transactionRepository).save(argThat(t ->
                    t.getStatus() == EnumStatus.CONCLUIDA
            ));
        }

        @Test
        @DisplayName("Should throw exception when transaction ID doesn't exist")
        void shouldThrowWhenTransactionNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(transactionRepository.existsById(nonExistentId)).thenReturn(false);

            Exception exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.completed(nonExistentId)
            );

            assertEquals("O Id não existe", exception.getMessage());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should maintain COMPLETED status when transaction is already completed")
        void shouldMaintainStatusWhenAlreadyCompleted() {
            when(transactionRepository.existsById(transactionId)).thenReturn(true);
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(completedTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            transactionService.completed(transactionId);

            verify(transactionRepository).save(argThat(t ->
                    t.getStatus() == EnumStatus.CONCLUIDA
            ));
        }

        @Test
        @DisplayName("Should throw exception when repository save fails")
        void shouldThrowWhenSaveFails() {
            when(transactionRepository.existsById(transactionId)).thenReturn(true);
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(pendingTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenThrow(new RuntimeException("Database error"));

            assertThrows(RuntimeException.class,
                    () -> transactionService.completed(transactionId));
        }
    }

    @Nested
    @DisplayName("failTransaction() Tests")
    class FailTransactionTests {

        @Test
        @DisplayName("Should change status to FAILED when transaction exists")
        void shouldChangeStatusToFailed() {
            when(transactionRepository.existsById(transactionId)).thenReturn(true);
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(pendingTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            transactionService.failure(transactionId);

            verify(transactionRepository).save(argThat(t ->
                    t.getStatus() == EnumStatus.FALHOU
            ));
        }

        @Test
        @DisplayName("Should throw exception when transaction ID doesn't exist")
        void shouldThrowWhenTransactionNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(transactionRepository.existsById(nonExistentId)).thenReturn(false);

            Exception exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> transactionService.failure(nonExistentId)
            );

            assertEquals("O Id não existe", exception.getMessage());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should maintain FAILED status when transaction is already failed")
        void shouldMaintainStatusWhenAlreadyFailed() {
            when(transactionRepository.existsById(transactionId)).thenReturn(true);
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(failedTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            transactionService.failure(transactionId);

            verify(transactionRepository).save(argThat(t ->
                    t.getStatus() == EnumStatus.FALHOU
            ));
        }

        @Test
        @DisplayName("Should verify correct repository call sequence")
        void shouldVerifyRepositoryCallSequence() {
            when(transactionRepository.existsById(transactionId)).thenReturn(true);
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(pendingTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            transactionService.failure(transactionId);

            InOrder inOrder = inOrder(transactionRepository);
            inOrder.verify(transactionRepository).existsById(transactionId);
            inOrder.verify(transactionRepository).findById(transactionId);
            inOrder.verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Should throw exception when repository save fails")
        void shouldThrowWhenSaveFails() {
            when(transactionRepository.existsById(transactionId)).thenReturn(true);
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(pendingTransaction));
            when(transactionRepository.save(any(Transaction.class))).thenThrow(new RuntimeException("Database error"));

            assertThrows(RuntimeException.class,
                    () -> transactionService.failure(transactionId));
        }
    }
}