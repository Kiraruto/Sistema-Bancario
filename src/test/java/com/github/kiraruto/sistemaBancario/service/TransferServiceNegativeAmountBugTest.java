package com.github.kiraruto.sistemaBancario.service;
import com.github.kiraruto.sistemaBancario.service.TransferService;

import com.github.kiraruto.sistemaBancario.dto.TransferRequestDTO;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.enums.AccountType;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Candidato #3: TransferService aceita valor negativo e inverte o fluxo do dinheiro.
 *
 * Contrato esperado: uma transferencia com valor negativo deve ser rejeitada.
 * No codigo atual debit() faz "saldo - (valor negativo)" (remetente ganha) e
 * credit() faz "saldo + (valor negativo)" (destinatario perde), sem checagem de sinal.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferServiceNegativeAmountBugTest {

    @Mock
    private CheckingAccountRepository checkingAccountRepository;

    @Mock
    private SavingsAccountRepository savingAccountRepository;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(checkingAccountRepository, savingAccountRepository);
    }

    @Test
    void negativeAmountShouldBeRejected() {
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();

        CheckingAccount from = new CheckingAccount();
        from.setId(fromId);
        from.setBalance(BigDecimal.valueOf(1000));

        CheckingAccount to = new CheckingAccount();
        to.setId(toId);
        to.setBalance(BigDecimal.valueOf(500));

        when(checkingAccountRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(checkingAccountRepository.findById(toId)).thenReturn(Optional.of(to));

        TransferRequestDTO dto = new TransferRequestDTO(
                fromId, AccountType.CHECKING,
                toId, AccountType.CHECKING,
                BigDecimal.valueOf(-100)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transfer(dto),
                "Transferencia com valor negativo deveria ser rejeitada"
        );
    }
}
