package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.*;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumMaritalStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTypeDocument;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.utils.SavingsAccountValidate;
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
class SavingsAccountServiceTest {

    @InjectMocks
    private SavingsAccountService savingsAccountService;

    @Mock
    private SavingsAccountRepository savingsAccountRepository;

    @Mock
    private SavingsAccountValidate savingsAccountValidate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionValidate transactionValidate;

    private final UUID accountId = UUID.randomUUID();
    private SavingsAccount savingsAccount;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("João Silva");
        user.setEmail("joao@email.com");

        savingsAccount = new SavingsAccount();
        savingsAccount.setId(accountId);
        savingsAccount.setBalance(BigDecimal.valueOf(1000));
        savingsAccount.setIsActive(true);
        savingsAccount.setUser(user);
    }

    @Nested
    @DisplayName("createAccount()")
    class CreateAccountTests {
        @Test
        @DisplayName("Deve criar conta com sucesso")
        void shouldCreateAccountSuccessfully() {
            SavingsAccountDTO dto = new SavingsAccountDTO(
                    "João Silva",
                    LocalDate.of(1990, 1, 1),
                    "Rua A",
                    EnumMaritalStatus.SOLTEIRO,
                    "12345678901",
                    "joao@email.com",
                    "123456789",
                    EnumTypeDocument.RG,
                    "1234567",
                    BigDecimal.valueOf(100),
                    user.getId()
            );

            SavingsAccount newAccount = new SavingsAccount(dto);
            newAccount.setId(savingsAccountRepository.findIdByFullNameAndBalance(dto.fullName(),dto.balance()));
            newAccount.setUser(user);

            when(userRepository.findById(dto.userId())).thenReturn(Optional.of(user));
            when(savingsAccountRepository.save(any(SavingsAccount.class))).thenReturn(newAccount);
            when(savingsAccountRepository.findByCpfAndEmail(dto.cpf(), dto.email()))
                    .thenReturn(Optional.of(newAccount));

            SavingsAccount result = savingsAccountService.createAccount(dto);

            assertNotNull(result);
            assertEquals(newAccount.getId(), result.getId());
            verify(userRepository).save(user);
            verify(savingsAccountRepository).save(any(SavingsAccount.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário não existir")
        void shouldThrowWhenUserNotFound() {
            SavingsAccountDTO dto = new SavingsAccountDTO(
                    "João Silva",
                    LocalDate.of(1990, 1, 1),
                    "Rua A",
                    EnumMaritalStatus.SOLTEIRO,
                    "12345678901",
                    "joao@email.com",
                    "123456789",
                    EnumTypeDocument.RG,
                    "1234567",
                    BigDecimal.valueOf(100),
                    user.getId()
            );

            when(userRepository.findById(any())).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> {
                savingsAccountService.createAccount(dto);
            });
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {
        @Test
        @DisplayName("Deve retornar conta quando existir")
        void shouldReturnAccountWhenExists() {
            when(savingsAccountRepository.findById(accountId)).thenReturn(Optional.of(savingsAccount));

            SavingsAccount result = savingsAccountService.getById(accountId);

            assertNotNull(result);
            assertEquals(savingsAccount, result);
        }

        @Test
        @DisplayName("Deve lançar exceção quando conta não existir")
        void shouldThrowWhenAccountNotFound() {
            when(savingsAccountRepository.findById(accountId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> {
                savingsAccountService.getById(accountId);
            });
        }
    }

    @Nested
    @DisplayName("transferSavingsAccount()")
    class TransferTests {
        @Test
        @DisplayName("Deve transferir com sucesso")
        void shouldTransferSuccessfully() {
            UUID receiverId = UUID.randomUUID();
            DepositRequestDTO request = new DepositRequestDTO(
                    "João Silva", "joao@email.com", "12345678901",
                    BigDecimal.valueOf(100), EnumOrigin.DOC,
                    accountId, receiverId, "Transferência", UUID.randomUUID());

            SavingsAccount receiver = new SavingsAccount();
            receiver.setId(receiverId);
            receiver.setBalance(BigDecimal.valueOf(500));
            receiver.setIsActive(true);

            when(savingsAccountRepository.findById(accountId)).thenReturn(Optional.of(savingsAccount));
            when(savingsAccountRepository.findById(receiverId)).thenReturn(Optional.of(receiver));

            savingsAccountService.transferSavingsAccount(request);

            assertEquals(BigDecimal.valueOf(900), savingsAccount.getBalance());
            assertEquals(BigDecimal.valueOf(600), receiver.getBalance());
            verify(transactionRepository).save(any(Transaction.class));
        }
    }

    @Nested
    @DisplayName("withdraw()")
    class WithdrawTests {
        @Test
        @DisplayName("Deve depositar com sucesso")
        void shouldDepositSuccessfully() {
            WithdrawRequestDTO request = new WithdrawRequestDTO(
                    accountId, BigDecimal.valueOf(100), EnumOrigin.DOC);

            when(savingsAccountValidate.validateSavingsAccountWithdraw(request))
                    .thenReturn(savingsAccount);
            when(savingsAccountRepository.findById(accountId))
                    .thenReturn(Optional.of(savingsAccount));

            savingsAccountService.withdraw(accountId, request);

            assertEquals(BigDecimal.valueOf(1100), savingsAccount.getBalance());
            verify(transactionRepository).save(any(Transaction.class));
        }
    }

    @Nested
    @DisplayName("withdrawal()")
    class WithdrawalTests {
        @Test
        @DisplayName("Deve sacar com sucesso")
        void shouldWithdrawSuccessfully() {
            WithdrawalRequestDTO request = new WithdrawalRequestDTO(
                    accountId, BigDecimal.valueOf(100), EnumOrigin.DOC);

            when(savingsAccountValidate.validateSavingsAccountWithdrawal(request))
                    .thenReturn(savingsAccount);
            when(savingsAccountRepository.findById(accountId))
                    .thenReturn(Optional.of(savingsAccount));

            savingsAccountService.withdrawal(accountId, request);

            assertEquals(BigDecimal.valueOf(900), savingsAccount.getBalance());
            verify(transactionRepository).save(any(Transaction.class));
        }
    }

    @Nested
    @DisplayName("getBalance()")
    class GetBalanceTests {
        @Test
        @DisplayName("Deve retornar saldo quando conta ativa")
        void shouldReturnBalanceWhenAccountActive() {
            BalanceDTO balanceDTO = new BalanceDTO(
                    "João Silva", "joao@email.com", "12345678901",
                    BigDecimal.valueOf(1000), true);

            when(savingsAccountRepository.findFullNameAndBalanceById(accountId))
                    .thenReturn(balanceDTO);

            BalanceDTO result = savingsAccountService.getBalance(accountId);

            assertEquals(balanceDTO, result);
        }

        @Test
        @DisplayName("Deve lançar exceção quando conta inativa")
        void shouldThrowWhenAccountInactive() {
            BalanceDTO balanceDTO = new BalanceDTO(
                    "João Silva", "joao@email.com", "12345678901",
                    BigDecimal.valueOf(1000), false);

            when(savingsAccountRepository.findFullNameAndBalanceById(accountId))
                    .thenReturn(balanceDTO);

            assertThrows(IllegalArgumentException.class, () -> {
                savingsAccountService.getBalance(accountId);
            });
        }
    }

    @Nested
    @DisplayName("disableSavingsAccount()")
    class DisableAccountTests {
        @Test
        @DisplayName("Deve desativar conta com sucesso")
        void shouldDisableAccountSuccessfully() {
            when(savingsAccountValidate.validateSavingsAccountToFalse(accountId))
                    .thenReturn(savingsAccount);

            savingsAccountService.disableSavingsAccount(accountId);

            assertFalse(savingsAccount.getIsActive());
            verify(savingsAccountRepository).save(savingsAccount);
        }
    }

    @Nested
    @DisplayName("activateSavingsAccount()")
    class ActivateAccountTests {
        @Test
        @DisplayName("Deve ativar conta com sucesso")
        void shouldActivateAccountSuccessfully() {
            savingsAccount.setIsActive(false);

            when(savingsAccountValidate.validateSavingsAccountToTrue(accountId))
                    .thenReturn(savingsAccount);

            savingsAccountService.activateSavingsAccount(accountId);

            assertTrue(savingsAccount.getIsActive());
            verify(savingsAccountRepository).save(savingsAccount);
        }
    }

    @Nested
    @DisplayName("getTransactions()")
    class GetTransactionsTests {
        @Test
        @DisplayName("Deve retornar transações quando conta existir")
        void shouldReturnTransactionsWhenAccountExists() {
            Transaction transaction = new Transaction();
            List<Transaction> transactions = List.of(transaction);

            when(savingsAccountRepository.existsById(accountId)).thenReturn(true);
            when(transactionRepository.findByAccountSends(accountId)).thenReturn(transactions);

            List<TransactionDTO> result = savingsAccountService.getTransactions(accountId);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Deve lançar exceção quando conta não existir")
        void shouldThrowWhenAccountNotExists() {
            when(savingsAccountRepository.existsById(accountId)).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () -> {
                savingsAccountService.getTransactions(accountId);
            });
        }
    }

    @Nested
    @DisplayName("getAll()")
    class GetAllTests {
        @Test
        @DisplayName("Deve retornar todas as contas")
        void shouldReturnAllAccounts() {
            List<SavingsAccount> accounts = List.of(savingsAccount);
            when(savingsAccountRepository.findAll()).thenReturn(accounts);

            List<SavingsAccount> result = savingsAccountService.getAll();

            assertEquals(1, result.size());
            assertEquals(savingsAccount, result.get(0));
        }
    }
}