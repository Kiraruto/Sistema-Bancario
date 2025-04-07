package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.*;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumMaritalStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTypeDocument;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.utils.CheckingAccountValidate;
import com.github.kiraruto.sistemaBancario.utils.interfaces.TransactionValidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckingAccountService Testes")
class CheckingAccountServiceTest {

    @InjectMocks
    private CheckingAccountService checkingAccountService;

    @Mock
    private CheckingAccountRepository checkingAccountRepository;

    @Mock
    private CheckingAccountValidate checkingAccountValidate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionValidate transactionValidate;

    private final UUID accountId = UUID.fromString("9044acfb-37f0-47ee-a850-a94586c4180a");
    private CheckingAccount checkingAccount;
    private User user;

    private CheckingAccountDTO createTestCheckingAccount(UUID userId, String nome, String mail, BigDecimal balance) {
        return new CheckingAccountDTO(
                nome,
                LocalDate.of(2005, 12, 8),
                "Rua a",
                EnumMaritalStatus.SOLTEIRO,
                "81993169673",
                mail,
                "43025634430",
                EnumTypeDocument.RG,
                "143400976",
                balance,
                false,
                false,
                userId
        );
    }

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("João Silva");
        user.setEmail("joao@email.com");

        checkingAccount = new CheckingAccount();
        checkingAccount.setId(accountId);
        checkingAccount.setIsActive(true);
        checkingAccount.setBalance(BigDecimal.valueOf(1000));
        checkingAccount.setUser(user);
    }

    @Nested
    @DisplayName("createAccount() Testes")
    class CreateAccountTests {
        @Test
        @DisplayName("Deve criar conta com sucesso")
        void shouldCreateAccountSuccessfully() {
            CheckingAccountDTO dto = createTestCheckingAccount(
                    user.getId(), "João", "joao@email.com", BigDecimal.valueOf(100));

            CheckingAccount newAccount = new CheckingAccount(dto);
            newAccount.setId(checkingAccountRepository.findIdByFullNameAndBalance(dto.fullName(),dto.balance()));
            newAccount.setUser(user);

            when(userRepository.findById(dto.userId())).thenReturn(Optional.of(user));
            when(checkingAccountRepository.save(any(CheckingAccount.class))).thenReturn(newAccount);
            when(checkingAccountRepository.findByCpfAndEmail(dto.cpf(), dto.email()))
                    .thenReturn(Optional.of(newAccount));

            CheckingAccount result = checkingAccountService.createAccount(dto);

            assertNotNull(result);
            assertEquals(newAccount.getId(), result.getId());
            verify(userRepository).save(user);
            verify(checkingAccountRepository).save(any(CheckingAccount.class));
        }

        @Test
        @DisplayName("Deve lançar exceção se usuário não for encontrado")
        void shouldThrowExceptionWhenUserNotFound() {
            CheckingAccountDTO dto = createTestCheckingAccount(
                    UUID.randomUUID(), "João", "j@j.com", BigDecimal.TEN);

            when(userRepository.findById(any())).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> {
                checkingAccountService.createAccount(dto);
            });

            verify(checkingAccountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAll() Testes")
    class GetAllTests {
        @Test
        @DisplayName("Deve retornar todas as contas")
        void shouldReturnAllAccounts() {
            List<CheckingAccount> accounts = List.of(checkingAccount);
            when(checkingAccountRepository.findAll()).thenReturn(accounts);

            List<CheckingAccount> result = checkingAccountService.getAll();

            assertEquals(1, result.size());
            assertEquals(checkingAccount, result.get(0));
        }
    }

    @Nested
    @DisplayName("getById() Testes")
    class GetByIdTests {
        @Test
        @DisplayName("Deve retornar a conta quando existir")
        void shouldReturnAccountWhenExists() {
            when(checkingAccountRepository.findById(accountId)).thenReturn(Optional.of(checkingAccount));

            CheckingAccount result = checkingAccountService.getById(accountId);

            assertNotNull(result);
            assertEquals(checkingAccount, result);
        }

        @Test
        @DisplayName("Deve lançar exceção quando conta não existir")
        void shouldThrowExceptionWhenAccountNotFound() {
            when(checkingAccountRepository.findById(accountId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> {
                checkingAccountService.getById(accountId);
            });
        }
    }

    @Nested
    @DisplayName("getTransactions() Testes")
    class GetTransactionsTests {
        @Test
        @DisplayName("Deve retornar transações quando conta existir")
        void shouldReturnTransactionsWhenAccountExists() {
            Transaction transaction = new Transaction();
            transaction.setAccountSends(checkingAccount.getId());
            List<Transaction> transactions = List.of(transaction);

            when(checkingAccountRepository.existsById(accountId)).thenReturn(true);
            when(transactionRepository.findByAccountSends(accountId)).thenReturn(transactions);

            List<TransactionDTO> result = checkingAccountService.getTransactions(accountId);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Deve lançar exceção quando conta não existir")
        void shouldThrowWhenAccountNotExists() {
            when(checkingAccountRepository.existsById(accountId)).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () -> {
                checkingAccountService.getTransactions(accountId);
            });
        }
    }

    @Nested
    @DisplayName("getBalance() Testes")
    class GetBalanceTests {
        @Test
        @DisplayName("Deve retornar saldo quando conta estiver ativa")
        void shouldReturnBalanceWhenAccountIsActive() {
            BalanceDTO balanceDTO = new BalanceDTO(
                    "João Silva", "joao@email.com", "81993169673",
                    BigDecimal.valueOf(1000), true);

            when(checkingAccountRepository.findFullNameAndBalanceById(accountId))
                    .thenReturn(balanceDTO);

            BalanceDTO result = checkingAccountService.getBalance(accountId);

            assertEquals(balanceDTO, result);
        }

        @Test
        @DisplayName("Deve lançar exceção quando conta estiver inativa")
        void shouldThrowWhenAccountIsInactive() {
            BalanceDTO balanceDTO = new BalanceDTO(
                    "João Silva", "joao@email.com", "81993169673",
                    BigDecimal.valueOf(1000), false);

            when(checkingAccountRepository.findFullNameAndBalanceById(accountId))
                    .thenReturn(balanceDTO);

            assertThrows(IllegalArgumentException.class, () -> {
                checkingAccountService.getBalance(accountId);
            });
        }
    }

    @Nested
    @DisplayName("withdraw() Testes")
    class WithdrawTests {
        @Test
        @DisplayName("Deve realizar saque com sucesso")
        void shouldWithdrawSuccessfully() {
            checkingAccount.setBalance(BigDecimal.valueOf(1000));

            WithdrawRequestDTO request = new WithdrawRequestDTO(
                    accountId, BigDecimal.valueOf(100), EnumOrigin.DOC);

            when(checkingAccountValidate.validateCheckingAccountWithdraw(request))
                    .thenReturn(checkingAccount);
            when(checkingAccountRepository.findById(accountId))
                    .thenReturn(Optional.of(checkingAccount));

            checkingAccountService.withdraw(accountId, request);

            assertEquals(BigDecimal.valueOf(1100), checkingAccount.getBalance());
            verify(transactionRepository).save(any(Transaction.class));
            verify(checkingAccountRepository).save(checkingAccount);
        }

        @Test
        @DisplayName("Deve lançar exceção quando conta não existir")
        void shouldThrowWhenAccountNotFound() {
            WithdrawRequestDTO request = new WithdrawRequestDTO(
                    accountId, BigDecimal.valueOf(100), EnumOrigin.DOC);

            when(checkingAccountRepository.findById(accountId))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> {
                checkingAccountService.withdraw(accountId, request);
            });
        }
    }

    @Nested
    @DisplayName("transferCheckingAccount() Testes")
    class TransferTests {
        @Test
        @DisplayName("Deve realizar transferência com sucesso")
        void shouldTransferSuccessfully() {
            UUID receiverId = UUID.randomUUID();
            DepositRequestDTO request = new DepositRequestDTO(
                    "João Silva", "joao@email.com", "81993169673",
                    BigDecimal.valueOf(100), EnumOrigin.DOC,
                    accountId, receiverId, "Transferência", UUID.randomUUID());

            CheckingAccount receiver = new CheckingAccount();
            receiver.setId(receiverId);
            receiver.setBalance(BigDecimal.valueOf(500));
            receiver.setIsActive(true);

            when(checkingAccountRepository.findById(accountId))
                    .thenReturn(Optional.of(checkingAccount));
            when(checkingAccountRepository.findById(receiverId))
                    .thenReturn(Optional.of(receiver));

            checkingAccountService.transferCheckingAccount(request);

            assertEquals(BigDecimal.valueOf(900), checkingAccount.getBalance());
            assertEquals(BigDecimal.valueOf(600), receiver.getBalance());
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando conta de origem não existir")
        void shouldThrowWhenSenderNotFound() {
            UUID receiverId = UUID.randomUUID();
            DepositRequestDTO request = new DepositRequestDTO(
                    "João Silva", "joao@email.com", "81993169673",
                    BigDecimal.valueOf(100), EnumOrigin.DOC,
                    accountId, receiverId, "Transferência", UUID.randomUUID());

            when(checkingAccountRepository.findById(accountId))
                    .thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> {
                checkingAccountService.transferCheckingAccount(request);
            });
        }
    }

    @Nested
    @DisplayName("withdrawal() Testes")
    class WithdrawalTests {
        @Test
        @DisplayName("Deve realizar saque de poupança com sucesso")
        void shouldWithdrawalSuccessfully() {
            WithdrawalRequestDTO request = new WithdrawalRequestDTO(
                    accountId, BigDecimal.valueOf(100), EnumOrigin.DOC);

            when(checkingAccountValidate.validateCheckingAccountWithdrawal(request))
                    .thenReturn(checkingAccount);
            when(checkingAccountRepository.findById(accountId))
                    .thenReturn(Optional.of(checkingAccount));

            checkingAccountService.withdrawal(accountId, request);

            assertEquals(BigDecimal.valueOf(900), checkingAccount.getBalance());
            verify(transactionRepository).save(any(Transaction.class));
        }
    }

    @Nested
    @DisplayName("disableCheckingAccount() Testes")
    class DisableAccountTests {
        @Test
        @DisplayName("Deve desativar conta com sucesso")
        void shouldDisableAccountSuccessfully() {
            when(checkingAccountValidate.validateCheckingAccountToFalse(accountId))
                    .thenReturn(checkingAccount);

            checkingAccountService.disableCheckingAccount(accountId);

            assertFalse(checkingAccount.getIsActive());
            verify(checkingAccountRepository).save(checkingAccount);
        }
    }

    @Nested
    @DisplayName("activateCheckingAccount() Testes")
    class ActivateAccountTests {
        @Test
        @DisplayName("Deve ativar conta com sucesso")
        void shouldActivateAccountSuccessfully() {
            checkingAccount.setIsActive(false);

            when(checkingAccountValidate.validateCheckingAccountToTrue(accountId))
                    .thenReturn(checkingAccount);

            checkingAccountService.activateCheckingAccount(accountId);

            assertTrue(checkingAccount.getIsActive());
            verify(checkingAccountRepository).save(checkingAccount);
        }
    }
}