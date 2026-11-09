package com.github.kiraruto.sistemaBancario.utils;

import com.github.kiraruto.sistemaBancario.dto.DepositRequestDTO;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.utils.CheckingAccountValidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

/**
 * Candidato: a transferencia entre contas correntes valida isActiveById passando
 * o id da CONTA em vez do id do USUARIO. Como a consulta devolve null,
 * !Boolean.TRUE.equals(null) e verdadeiro e a transferencia e rejeitada mesmo
 * com os dois usuarios ativos.
 *
 * Contrato esperado: usuarios ativos devem permitir a transferencia.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Bug: transferencia de conta corrente valida usuario pelo id da conta")
class CheckingTransferActiveUserBugTest {

    @Mock
    private CheckingAccountRepository checkingAccountRepository;

    @Mock
    private UserRepository userRepository;

    private CheckingAccountValidate checkingAccountValidate;

    @BeforeEach
    void setUp() {
        checkingAccountValidate = new CheckingAccountValidate(checkingAccountRepository, userRepository);
    }

    @Test
    @DisplayName("usuarios ativos devem permitir a transferencia")
    void activeUsersShouldAllowTransfer() {
        UUID senderAccountId = UUID.randomUUID();
        UUID receiverAccountId = UUID.randomUUID();
        UUID senderUserId = UUID.randomUUID();
        UUID receiverUserId = UUID.randomUUID();

        DepositRequestDTO request = new DepositRequestDTO(
                "Fulano de Tal",
                "fulano@example.com",
                "52998224725",
                BigDecimal.TEN,
                EnumOrigin.DINHEIRO_FISICO,
                senderAccountId,
                receiverAccountId,
                "Transferencia",
                senderUserId
        );

        when(checkingAccountRepository.existsByFullName(request.fullName())).thenReturn(true);
        when(checkingAccountRepository.existsByEmail(request.email())).thenReturn(true);
        when(checkingAccountRepository.existsByCpf(request.cpf())).thenReturn(true);
        when(checkingAccountRepository.existsByUserId(request.userId())).thenReturn(true);
        when(checkingAccountRepository.findById(senderAccountId))
                .thenReturn(Optional.of(account(senderAccountId, senderUserId)));
        when(checkingAccountRepository.findById(receiverAccountId))
                .thenReturn(Optional.of(account(receiverAccountId, receiverUserId)));
        when(userRepository.isActiveById(senderUserId)).thenReturn(true);
        when(userRepository.isActiveById(receiverUserId)).thenReturn(true);

        assertDoesNotThrow(() -> checkingAccountValidate.validateDepositChecking(request));
    }

    private CheckingAccount account(UUID accountId, UUID userId) {
        User user = new User();
        user.setId(userId);

        CheckingAccount account = new CheckingAccount();
        account.setId(accountId);
        account.setUser(user);
        return account;
    }
}
