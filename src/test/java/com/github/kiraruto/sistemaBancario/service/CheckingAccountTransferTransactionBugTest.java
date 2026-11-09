package com.github.kiraruto.sistemaBancario.service;
import com.github.kiraruto.sistemaBancario.service.CheckingAccountService;
import com.github.kiraruto.sistemaBancario.service.DepositValidatorService;
import com.github.kiraruto.sistemaBancario.service.WithdrawalValidatorService;
import com.github.kiraruto.sistemaBancario.service.AuditService;

import com.github.kiraruto.sistemaBancario.dto.DepositRequestDTO;
import com.github.kiraruto.sistemaBancario.mapper.CheckingAccountMapper;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.utils.CheckingAccountValidate;
import com.github.kiraruto.sistemaBancario.utils.DepositValidator;
import com.github.kiraruto.sistemaBancario.utils.TransactionValidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Candidato #4: transferencia entre contas CORRENTES usa a validacao de POUPANCA.
 *
 * Contrato esperado: transferir entre duas contas correntes existentes e ativas deve
 * concluir e registrar uma Transaction. O codigo atual monta
 * "new Transaction(depositRequestDTO, transactionValidate)", cujo construtor chama
 * TransactionValidate.deposit(), que busca a conta de destino em
 * savingsAccountRepository. Como o destino e uma conta corrente, lanca
 * AccountNotFoundException — depois que os saldos ja foram salvos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CheckingAccountTransferTransactionBugTest {

    @Mock
    private CheckingAccountRepository checkingAccountRepository;

    @Mock
    private CheckingAccountValidate checkingAccountValidate;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SavingsAccountRepository savingsAccountRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private DepositValidator depositValidator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WithdrawalValidatorService withdrawalValidatorService;

    @Mock
    private DepositValidatorService depositValidatorService;

    @Mock
    private CheckingAccountMapper checkingAccountMapper;

    private CheckingAccountService checkingAccountService;

    @BeforeEach
    void setUp() {
        TransactionValidate transactionValidate = new TransactionValidate(
                transactionRepository,
                savingsAccountRepository,
                auditService,
                depositValidator
        );

        checkingAccountService = new CheckingAccountService(
                checkingAccountRepository,
                checkingAccountValidate,
                transactionRepository,
                transactionValidate,
                userRepository,
                withdrawalValidatorService,
                depositValidatorService,
                checkingAccountMapper
        );
    }

    @Test
    void checkingToCheckingTransferShouldRegisterTransactionWithoutSavingsAccount() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        CheckingAccount sender = new CheckingAccount();
        sender.setId(senderId);
        sender.setBalance(BigDecimal.valueOf(1000));

        CheckingAccount receiver = new CheckingAccount();
        receiver.setId(receiverId);
        receiver.setBalance(BigDecimal.valueOf(500));

        when(checkingAccountRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(checkingAccountRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(savingsAccountRepository.findById(receiverId)).thenReturn(Optional.empty());

        DepositRequestDTO dto = new DepositRequestDTO(
                "Joao Silva",
                "joao@email.com",
                "43025634430",
                BigDecimal.valueOf(100),
                EnumOrigin.DOC,
                senderId,
                receiverId,
                "Transferencia entre contas correntes",
                UUID.randomUUID()
        );

        assertDoesNotThrow(
                () -> checkingAccountService.transferCheckingAccount(dto),
                "Transferencia conta corrente -> conta corrente deveria concluir"
        );

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(
                EnumTransactionType.TRANSFERENCIA,
                captor.getValue().getTransactionType(),
                "A transferencia entre contas correntes deve ser registrada como TRANSFERENCIA"
        );

        assertEquals(BigDecimal.valueOf(900), sender.getBalance());
        assertEquals(BigDecimal.valueOf(600), receiver.getBalance());
    }
}
