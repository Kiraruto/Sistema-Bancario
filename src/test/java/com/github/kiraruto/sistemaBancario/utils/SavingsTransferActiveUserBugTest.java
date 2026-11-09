package com.github.kiraruto.sistemaBancario.utils;

import com.github.kiraruto.sistemaBancario.dto.DepositRequestDTO;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.utils.SavingsAccountValidate;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Candidato: a transferencia entre poupancas valida isActiveById passando o id da
 * CONTA, mas a consulta espera o id do USUARIO. Como nao existe usuario com o id
 * da conta, o retorno e null e Boolean.FALSE.equals(null) == false: usuario
 * inativo passa despercebido.
 *
 * Contrato esperado: o usuario DONO da conta de origem inativo deve bloquear.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Bug: transferencia de poupanca valida usuario pelo id da conta")
class SavingsTransferActiveUserBugTest {

    @Mock
    private SavingsAccountRepository savingsAccountRepository;

    @Mock
    private UserRepository userRepository;

    private SavingsAccountValidate savingsAccountValidate;

    @BeforeEach
    void setUp() {
        savingsAccountValidate = new SavingsAccountValidate(savingsAccountRepository, userRepository);
    }

    @Test
    @DisplayName("usuario dono da conta de origem inativo deve bloquear transferencia")
    void inactiveSenderUserShouldRejectTransfer() {
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

        when(savingsAccountRepository.existsByFullNameAndEmailAndCpf(
                request.fullName(), request.email(), request.cpf())).thenReturn(true);
        when(savingsAccountRepository.findById(senderAccountId))
                .thenReturn(Optional.of(account(senderAccountId, senderUserId)));
        when(savingsAccountRepository.findById(receiverAccountId))
                .thenReturn(Optional.of(account(receiverAccountId, receiverUserId)));
        // O JPA devolve null quando nao existe usuario com o id informado (id de conta).
        when(userRepository.isActiveById(senderAccountId)).thenReturn(null);
        when(userRepository.isActiveById(receiverAccountId)).thenReturn(null);
        when(userRepository.isActiveById(senderUserId)).thenReturn(false);
        when(userRepository.isActiveById(receiverUserId)).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> savingsAccountValidate.validateDepositSavings(request)
        );
    }

    private SavingsAccount account(UUID accountId, UUID userId) {
        User user = new User();
        user.setId(userId);

        SavingsAccount account = new SavingsAccount();
        account.setId(accountId);
        account.setUser(user);
        return account;
    }
}
