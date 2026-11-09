package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.TransferRequestDTO;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.AccountType;
import com.github.kiraruto.sistemaBancario.model.enums.EnumMaritalStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTypeDocument;
import com.github.kiraruto.sistemaBancario.model.enums.EnumUserRole;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regressão do P0: a transferência precisa ser atômica e registrar a transação.
 *
 * Antes da correção o débito era salvo sem transação e uma falha no crédito deixava
 * o dinheiro desaparecer da conta de origem, sem qualquer registro de transferência.
 */
@DisplayName("TransferService - atomicidade e registro da transferência")
class TransferServiceTransactionTest extends AbstractIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private CheckingAccountRepository checkingAccountRepository;

    @Autowired
    private SavingsAccountRepository savingsAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private CheckingAccount source;
    private SavingsAccount target;
    private User owner;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        owner = new User();
        owner.setUsername("transfer-" + suffix);
        owner.setFirstName("Titular");
        owner.setLastName("Transferencia");
        owner.setEmail("transfer-" + suffix + "@example.com");
        owner.setPassword("password");
        owner.setRole(EnumUserRole.CLIENTE);
        owner.setActive(true);
        owner = userRepository.save(owner);

        source = new CheckingAccount(
                "Conta Origem",
                LocalDate.of(1990, 1, 1),
                "Rua Origem, 1",
                EnumMaritalStatus.SOLTEIRO,
                randomPhone(),
                "origem-" + suffix + "@example.com",
                randomValidCpf(),
                EnumTypeDocument.RG,
                "DOC" + suffix,
                BigDecimal.valueOf(1000),
                false,
                false,
                true
        );
        source.setUser(owner);
        source = checkingAccountRepository.save(source);

        target = new SavingsAccount(
                "Conta Destino",
                LocalDate.of(1990, 1, 1),
                "Rua Destino, 2",
                EnumMaritalStatus.SOLTEIRO,
                randomPhone(),
                "destino-" + suffix + "@example.com",
                randomValidCpf(),
                EnumTypeDocument.RG,
                "DOC" + suffix + "D",
                BigDecimal.valueOf(0.006743),
                BigDecimal.valueOf(500),
                true
        );
        target.setUser(owner);
        target = savingsAccountRepository.save(target);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        owner.getUsername(),
                        null,
                        List.of(new SimpleGrantedAuthority(EnumUserRole.CLIENTE.name()))
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("deve debitar, creditar e registrar a transferência concluída")
    void shouldTransferAndRegisterTransaction() {
        UUID sourceId = source.getId();
        UUID targetId = target.getId();

        transferService.transfer(new TransferRequestDTO(
                sourceId, AccountType.CHECKING,
                targetId, AccountType.SAVING,
                BigDecimal.valueOf(250)
        ));

        assertEquals(
                0,
                checkingAccountRepository.findById(sourceId).orElseThrow().getBalance()
                        .compareTo(BigDecimal.valueOf(750))
        );
        assertEquals(
                0,
                savingsAccountRepository.findById(targetId).orElseThrow().getBalance()
                        .compareTo(BigDecimal.valueOf(750))
        );

        List<Transaction> registered = transactionsBetween(sourceId, targetId);
        assertEquals(1, registered.size());
        Transaction transaction = registered.get(0);
        assertEquals(EnumTransactionType.TRANSFERENCIA, transaction.getTransactionType());
        assertEquals(EnumStatus.CONCLUIDA, transaction.getStatus());
        assertEquals(0, transaction.getAmount().compareTo(BigDecimal.valueOf(250)));
    }

    @Test
    @DisplayName("deve desfazer o débito quando o crédito falha")
    void shouldRollbackDebitWhenCreditFails() {
        UUID sourceId = source.getId();
        UUID targetId = target.getId();

        // Força um erro no banco ao creditar: estoura a precisão de DECIMAL(18,2).
        SavingsAccount overflowingTarget = savingsAccountRepository.findById(targetId).orElseThrow();
        overflowingTarget.setBalance(new BigDecimal("9999999999999999.99"));
        savingsAccountRepository.saveAndFlush(overflowingTarget);

        assertThrows(
                RuntimeException.class,
                () -> transferService.transfer(new TransferRequestDTO(
                        sourceId, AccountType.CHECKING,
                        targetId, AccountType.SAVING,
                        BigDecimal.valueOf(100)
                ))
        );

        assertEquals(
                0,
                checkingAccountRepository.findById(sourceId).orElseThrow().getBalance()
                        .compareTo(BigDecimal.valueOf(1000)),
                "O débito não pode persistir quando o crédito falha"
        );
        assertTrue(
                transactionsBetween(sourceId, targetId).isEmpty(),
                "Não deve existir registro de transferência quando a operação falha"
        );
    }

    private List<Transaction> transactionsBetween(UUID sourceId, UUID targetId) {
        return transactionRepository.findAll().stream()
                .filter(t -> sourceId.equals(t.getAccountSends()) && targetId.equals(t.getAccountReceive()))
                .toList();
    }

    private String randomPhone() {
        return "119" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    private String randomValidCpf() {
        int[] digits = new int[11];
        for (int i = 0; i < 9; i++) {
            digits[i] = ThreadLocalRandom.current().nextInt(10);
        }
        for (int position = 9; position < 11; position++) {
            int sum = 0;
            for (int i = 0; i < position; i++) {
                sum += digits[i] * (position + 1 - i);
            }
            int digit = (sum * 10) % 11;
            digits[position] = digit == 10 ? 0 : digit;
        }
        StringBuilder cpf = new StringBuilder();
        for (int digit : digits) {
            cpf.append(digit);
        }
        return cpf.toString();
    }
}
