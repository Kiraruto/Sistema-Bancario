package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.model.SavingsAccount;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumMaritalStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTypeDocument;
import com.github.kiraruto.sistemaBancario.model.enums.EnumUserRole;
import com.github.kiraruto.sistemaBancario.repository.SavingsAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regressão do P1: sem controle de versão, duas transações que leem o mesmo
 * saldo e gravam em seguida podiam sobrescrever uma à outra (lost update).
 * Com @Version, a segunda gravação falha em vez de perder a primeira.
 */
@DisplayName("Account - bloqueio otimista")
class AccountOptimisticLockingTest extends AbstractIntegrationTest {

    @Autowired
    private SavingsAccountRepository savingsAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("atualização sobre versão desatualizada deve falhar em vez de sobrescrever")
    void staleUpdateShouldFailInsteadOfOverwriting() {
        UUID accountId = createAccount().getId();

        SavingsAccount stale = transactionTemplate.execute(
                status -> savingsAccountRepository.findById(accountId).orElseThrow()
        );
        assertNotNull(stale, "A conta deve ter sido lida na primeira transação");
        assertEquals(0L, stale.getVersion(), "A conta recém-criada começa na versão 0");

        transactionTemplate.executeWithoutResult(status -> {
            SavingsAccount fresh = savingsAccountRepository.findById(accountId).orElseThrow();
            fresh.setBalance(BigDecimal.valueOf(2000));
            savingsAccountRepository.saveAndFlush(fresh);
        });

        assertEquals(
                1L,
                savingsAccountRepository.findById(accountId).orElseThrow().getVersion(),
                "A atualização vencedora deve incrementar a versão"
        );

        stale.setBalance(BigDecimal.valueOf(3000));

        assertThrows(
                OptimisticLockingFailureException.class,
                () -> transactionTemplate.executeWithoutResult(
                        status -> savingsAccountRepository.saveAndFlush(stale)
                ),
                "Gravar uma versão desatualizada não pode sobrescrever o saldo atual"
        );

        assertEquals(
                0,
                savingsAccountRepository.findById(accountId).orElseThrow()
                        .getBalance().compareTo(BigDecimal.valueOf(2000)),
                "O saldo deve permanecer o da atualização vencedora"
        );
    }

    private SavingsAccount createAccount() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        User owner = new User();
        owner.setUsername("optimistic-" + suffix);
        owner.setFirstName("Titular");
        owner.setLastName("Concorrencia");
        owner.setEmail("optimistic-" + suffix + "@example.com");
        owner.setPassword("password");
        owner.setRole(EnumUserRole.CLIENTE);
        owner.setActive(true);
        owner = userRepository.save(owner);

        SavingsAccount account = new SavingsAccount(
                "Conta Concorrência",
                LocalDate.of(1990, 1, 1),
                "Rua Concorrência, 1",
                EnumMaritalStatus.SOLTEIRO,
                randomPhone(),
                "conta-" + suffix + "@example.com",
                randomValidCpf(),
                EnumTypeDocument.RG,
                "DOC" + suffix,
                BigDecimal.valueOf(0.006743),
                BigDecimal.valueOf(1000),
                true
        );
        account.setUser(owner);
        return savingsAccountRepository.saveAndFlush(account);
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
