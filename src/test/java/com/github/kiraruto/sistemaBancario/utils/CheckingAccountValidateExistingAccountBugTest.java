package com.github.kiraruto.sistemaBancario.utils;
import com.github.kiraruto.sistemaBancario.utils.CheckingAccountValidate;

import com.github.kiraruto.sistemaBancario.dto.WithdrawRequestDTO;
import com.github.kiraruto.sistemaBancario.dto.WithdrawalRequestDTO;
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
 * Candidato #2: deposito/saque em conta corrente existente e rejeitado como "nao existe".
 *
 * Contrato esperado: se a conta existe (existsByFullNameAndEmailAndCpf == true) e o
 * usuario esta ativo, a validacao deve aceitar a operacao. O codigo atual lanca
 * "Nao existe conta..." justamente quando exists == true (negacao ausente).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CheckingAccountValidateExistingAccountBugTest {

    @Mock
    private CheckingAccountRepository checkingAccountRepository;

    @Mock
    private UserRepository userRepository;

    private CheckingAccountValidate validate;
    private UUID accountId;
    private UUID userId;
    private CheckingAccount account;

    @BeforeEach
    void setUp() {
        validate = new CheckingAccountValidate(checkingAccountRepository, userRepository);

        accountId = UUID.randomUUID();
        userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        account = new CheckingAccount();
        account.setId(accountId);
        account.setIsActive(true);
        account.setFullName("Joao Silva");
        account.setEmail("joao@email.com");
        account.setCpf("43025634430");
        account.setUser(user);

        when(checkingAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(checkingAccountRepository.existsByFullNameAndEmailAndCpf(
                account.getFullName(), account.getEmail(), account.getCpf())).thenReturn(true);
        when(userRepository.isActiveById(userId)).thenReturn(true);
    }

    @Test
    void depositOnExistingAccountShouldBeAccepted() {
        WithdrawRequestDTO dto = new WithdrawRequestDTO(accountId, BigDecimal.TEN, EnumOrigin.DOC);

        assertDoesNotThrow(
                () -> validate.validateCheckingAccountWithdraw(dto),
                "Deposito em conta existente e ativa deveria ser aceito"
        );
    }

    @Test
    void withdrawalOnExistingAccountShouldBeAccepted() {
        WithdrawalRequestDTO dto = new WithdrawalRequestDTO(accountId, BigDecimal.TEN, EnumOrigin.DOC);

        assertDoesNotThrow(
                () -> validate.validateCheckingAccountWithdrawal(dto),
                "Saque em conta existente e ativa deveria ser aceito"
        );
    }
}
