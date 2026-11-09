package com.github.kiraruto.sistemaBancario.utils;

import com.github.kiraruto.sistemaBancario.dto.DepositRequestDTO;
import com.github.kiraruto.sistemaBancario.dto.WithdrawRequestDTO;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.service.AMLService;
import com.github.kiraruto.sistemaBancario.service.BankHoursService;
import com.github.kiraruto.sistemaBancario.utils.DepositValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

/**
 * Candidato: validateDeposito compara List.of("AGENCIA","ATM") (String) com
 * EnumOrigin, entao a comparacao nunca casa e todo deposito em DINHEIRO_FISICO
 * e rejeitado com "Origem do deposito invalida".
 *
 * Contrato esperado: a origem DINHEIRO_FISICO, unica que chega nesse fluxo,
 * deve ser aceita.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Bug: validacao de origem compara String com EnumOrigin")
class DepositValidatorOriginBugTest {

    @Mock
    private BankHoursService bankHoursService;

    @Mock
    private AMLService amlService;

    @Mock
    private SavingsAccountRepository savingsAccountRepository;

    @InjectMocks
    private DepositValidator depositValidator;

    @Test
    @DisplayName("deposito em dinheiro fisico deve ser aceito")
    void cashDepositShouldAcceptValidOrigin() {
        DepositRequestDTO request = new DepositRequestDTO(
                "Fulano de Tal",
                "fulano@example.com",
                "52998224725",
                BigDecimal.TEN,
                EnumOrigin.DINHEIRO_FISICO,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Deposito em dinheiro",
                UUID.randomUUID()
        );

        assertDoesNotThrow(() -> depositValidator.validateDeposito(request));
    }

    @Test
    @DisplayName("deposito em dinheiro fisico via transacao deve ser aceito")
    void cashDepositTransactionShouldAcceptValidOrigin() {
        UUID accountId = UUID.randomUUID();
        SavingsAccount account = new SavingsAccount();
        account.setId(accountId);
        account.setCpf("52998224725");

        when(savingsAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertDoesNotThrow(() -> depositValidator.validateDepositoTransiction(
                new WithdrawRequestDTO(accountId, BigDecimal.TEN, EnumOrigin.DINHEIRO_FISICO)
        ));
    }
}
