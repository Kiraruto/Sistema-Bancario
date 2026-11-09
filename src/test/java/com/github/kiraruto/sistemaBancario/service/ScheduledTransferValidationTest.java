package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.ScheduledTransferDTO;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.ScheduledTransfer;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.ScheduledTransferRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.utils.ValidateScheduleTransfer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regressão do P1: o agendamento só pode ser criado quando as contas existem,
 * estão ativas, são diferentes e o valor/data são válidos. Antes, o serviço
 * persistia o agendamento e a falha só aparecia na execução.
 */
@DisplayName("ScheduledTransfer - validação na criação")
@ExtendWith(MockitoExtension.class)
class ScheduledTransferValidationTest {

    @Mock
    private SavingsAccountRepository savingsAccountRepository;

    @Mock
    private ScheduledTransferRepository scheduledTransferRepository;

    @Mock
    private ScheduledTransferExecutor scheduledTransferExecutor;

    @Mock
    private UserRepository userRepository;

    private ScheduledTransferService service;
    private UUID sourceId;
    private UUID targetId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new ScheduledTransferService(
                new ValidateScheduleTransfer(savingsAccountRepository),
                scheduledTransferRepository,
                scheduledTransferExecutor,
                userRepository
        );
        sourceId = UUID.randomUUID();
        targetId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("conta de origem inexistente deve ser rejeitada antes de salvar")
    void shouldRejectMissingSourceAccount() {
        when(savingsAccountRepository.findById(sourceId)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createScheduledTransfer(validDto())
        );

        assertEquals("Conta de origem não encontrada", error.getMessage());
        verify(scheduledTransferRepository, never()).save(any());
    }

    @Test
    @DisplayName("conta de destino inexistente deve ser rejeitada antes de salvar")
    void shouldRejectMissingTargetAccount() {
        when(savingsAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(account(sourceId, true)));
        when(savingsAccountRepository.findById(targetId)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createScheduledTransfer(validDto())
        );

        assertEquals("Conta de destino não encontrada", error.getMessage());
        verify(scheduledTransferRepository, never()).save(any());
    }

    @Test
    @DisplayName("origem igual ao destino deve ser rejeitada")
    void shouldRejectSameAccount() {
        ScheduledTransferDTO dto = new ScheduledTransferDTO(
                sourceId, sourceId, BigDecimal.TEN,
                LocalDateTime.now().plusDays(1), userId.toString()
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createScheduledTransfer(dto)
        );

        assertEquals("A conta de origem e a conta de destino devem ser diferentes", error.getMessage());
        verify(scheduledTransferRepository, never()).save(any());
    }

    @Test
    @DisplayName("conta de origem inativa deve ser rejeitada")
    void shouldRejectInactiveSourceAccount() {
        when(savingsAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(account(sourceId, false)));
        when(savingsAccountRepository.findById(targetId))
                .thenReturn(Optional.of(account(targetId, true)));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createScheduledTransfer(validDto())
        );

        assertEquals("A conta de origem está inativa", error.getMessage());
        verify(scheduledTransferRepository, never()).save(any());
    }

    @Test
    @DisplayName("conta de destino inativa deve ser rejeitada")
    void shouldRejectInactiveTargetAccount() {
        when(savingsAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(account(sourceId, true)));
        when(savingsAccountRepository.findById(targetId))
                .thenReturn(Optional.of(account(targetId, false)));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createScheduledTransfer(validDto())
        );

        assertEquals("A conta de destino está inativa", error.getMessage());
        verify(scheduledTransferRepository, never()).save(any());
    }

    @Test
    @DisplayName("valor não positivo deve ser rejeitado")
    void shouldRejectNonPositiveAmount() {
        when(savingsAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(account(sourceId, true)));
        when(savingsAccountRepository.findById(targetId))
                .thenReturn(Optional.of(account(targetId, true)));

        ScheduledTransferDTO dto = new ScheduledTransferDTO(
                sourceId, targetId, BigDecimal.ZERO,
                LocalDateTime.now().plusDays(1), userId.toString()
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createScheduledTransfer(dto)
        );

        assertEquals("O valor do agendamento deve ser positivo", error.getMessage());
        verify(scheduledTransferRepository, never()).save(any());
    }

    @Test
    @DisplayName("data no passado deve ser rejeitada")
    void shouldRejectPastDate() {
        when(savingsAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(account(sourceId, true)));
        when(savingsAccountRepository.findById(targetId))
                .thenReturn(Optional.of(account(targetId, true)));

        ScheduledTransferDTO dto = new ScheduledTransferDTO(
                sourceId, targetId, BigDecimal.TEN,
                LocalDateTime.now().minusMinutes(1), userId.toString()
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createScheduledTransfer(dto)
        );

        assertEquals("A data do agendamento deve ser futura", error.getMessage());
        verify(scheduledTransferRepository, never()).save(any());
    }

    @Test
    @DisplayName("agendamento válido deve ser persistido como PENDENTE")
    void shouldPersistValidScheduledTransfer() {
        when(savingsAccountRepository.findById(sourceId))
                .thenReturn(Optional.of(account(sourceId, true)));
        when(savingsAccountRepository.findById(targetId))
                .thenReturn(Optional.of(account(targetId, true)));
        when(scheduledTransferRepository.save(any(ScheduledTransfer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduledTransfer saved = service.createScheduledTransfer(validDto());

        assertEquals(sourceId, saved.getSourceAccount());
        assertEquals(targetId, saved.getTargetAccount());
        assertEquals(0, saved.getAmount().compareTo(BigDecimal.TEN));
        assertEquals(EnumStatus.PENDENTE, saved.getStatus());
        verify(scheduledTransferRepository).save(any(ScheduledTransfer.class));
    }

    private ScheduledTransferDTO validDto() {
        return new ScheduledTransferDTO(
                sourceId, targetId, BigDecimal.TEN,
                LocalDateTime.now().plusDays(1), userId.toString()
        );
    }

    private SavingsAccount account(UUID id, boolean active) {
        SavingsAccount account = new SavingsAccount();
        account.setId(id);
        account.setIsActive(active);
        return account;
    }
}
