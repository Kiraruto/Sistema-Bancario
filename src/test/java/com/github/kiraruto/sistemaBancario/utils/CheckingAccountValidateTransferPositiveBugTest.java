package com.github.kiraruto.sistemaBancario.utils;
import com.github.kiraruto.sistemaBancario.utils.CheckingAccountValidate;

import com.github.kiraruto.sistemaBancario.dto.DepositRequestDTO;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

/**
 * Candidato #1: transferencia entre contas correntes rejeita todo valor positivo.
 *
 * Contrato esperado: um valor positivo, com contas/usuarios validos, deve ser aceito
 * pela validacao. O equivalente de poupanca (SavingsAccountValidate) usa "<= 0" para
 * rejeitar, enquanto CheckingAccountValidate usa ">= 0", invertendo a regra.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CheckingAccountValidateTransferPositiveBugTest {

    @Mock
    private CheckingAccountRepository checkingAccountRepository;

    @Mock
    private UserRepository userRepository;

    private CheckingAccountValidate validate;

    @BeforeEach
    void setUp() {
        validate = new CheckingAccountValidate(checkingAccountRepository, userRepository);
    }

    @Test
    void positiveTransferAmountShouldBeAccepted() {
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID receiverUserId = UUID.randomUUID();

        User senderUser = new User();
        senderUser.setId(userId);

        User receiverUser = new User();
        receiverUser.setId(receiverUserId);

        CheckingAccount senderAccount = new CheckingAccount();
        senderAccount.setId(sender);
        senderAccount.setUser(senderUser);

        CheckingAccount receiverAccount = new CheckingAccount();
        receiverAccount.setId(receiver);
        receiverAccount.setUser(receiverUser);

        DepositRequestDTO dto = new DepositRequestDTO(
                "Joao Silva",
                "joao@email.com",
                "43025634430",
                BigDecimal.valueOf(100),
                EnumOrigin.DOC,
                sender,
                receiver,
                "Transferencia entre contas",
                userId
        );

        when(checkingAccountRepository.existsByFullName(dto.fullName())).thenReturn(true);
        when(checkingAccountRepository.existsByEmail(dto.email())).thenReturn(true);
        when(checkingAccountRepository.existsByCpf(dto.cpf())).thenReturn(true);
        when(checkingAccountRepository.findById(sender)).thenReturn(Optional.of(senderAccount));
        when(checkingAccountRepository.findById(receiver)).thenReturn(Optional.of(receiverAccount));
        when(userRepository.isActiveById(userId)).thenReturn(true);
        when(userRepository.isActiveById(receiverUserId)).thenReturn(true);
        when(checkingAccountRepository.existsByUserId(userId)).thenReturn(true);

        assertDoesNotThrow(
                () -> validate.validateDepositChecking(dto),
                "Transferencia com valor positivo deveria ser aceita"
        );
    }
}
