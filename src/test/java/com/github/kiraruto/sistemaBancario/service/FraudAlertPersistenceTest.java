package com.github.kiraruto.sistemaBancario.service;

import com.github.kiraruto.sistemaBancario.dto.WithdrawRequestDTO;
import com.github.kiraruto.sistemaBancario.exceptions.DepositLimitExceededException;
import com.github.kiraruto.sistemaBancario.model.AlertAML;
import com.github.kiraruto.sistemaBancario.model.CheckingAccount;
import com.github.kiraruto.sistemaBancario.model.Transaction;
import com.github.kiraruto.sistemaBancario.model.User;
import com.github.kiraruto.sistemaBancario.model.enums.EnumMaritalStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumOrigin;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatus;
import com.github.kiraruto.sistemaBancario.model.enums.EnumStatusAlert;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTransactionType;
import com.github.kiraruto.sistemaBancario.model.enums.EnumTypeDocument;
import com.github.kiraruto.sistemaBancario.model.enums.EnumUserRole;
import com.github.kiraruto.sistemaBancario.repository.AlertAMLRepository;
import com.github.kiraruto.sistemaBancario.repository.CheckingAccountRepository;
import com.github.kiraruto.sistemaBancario.repository.TransactionRepository;
import com.github.kiraruto.sistemaBancario.repository.UserRepository;
import com.github.kiraruto.sistemaBancario.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regressão do P0: alertas de AML/fraude precisam sobreviver ao rollback do depósito.
 *
 * Antes da correção os alertas eram gravados na mesma transação do depósito e
 * desapareciam quando a exceção de limite/fraude provocava o rollback.
 */
@DisplayName("AuditService - alertas sobrevivem ao rollback do depósito")
class FraudAlertPersistenceTest extends AbstractIntegrationTest {

    @MockitoBean
    private KafkaTemplate<String, AlertAML> kafkaTemplate;

    @Autowired
    private CheckingAccountService checkingAccountService;

    @Autowired
    private CheckingAccountRepository checkingAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AlertAMLRepository alertAMLRepository;

    @Autowired
    private UserRepository userRepository;

    private CheckingAccount account;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        User owner = new User();
        owner.setUsername("fraud-" + suffix);
        owner.setFirstName("Titular");
        owner.setLastName("Fraude");
        owner.setEmail("fraud-" + suffix + "@example.com");
        owner.setPassword("password");
        owner.setRole(EnumUserRole.CLIENTE);
        owner.setActive(true);
        owner = userRepository.save(owner);

        account = new CheckingAccount(
                "Conta Fraude",
                LocalDate.of(1990, 1, 1),
                "Rua Fraude, 1",
                EnumMaritalStatus.SOLTEIRO,
                randomPhone(),
                "conta-" + suffix + "@example.com",
                randomValidCpf(),
                EnumTypeDocument.RG,
                "DOC" + suffix,
                BigDecimal.valueOf(1000),
                false,
                false,
                true
        );
        account.setUser(owner);
        account = checkingAccountRepository.save(account);

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
    @DisplayName("alerta de depósito acima de R$ 50.000 deve persistir após o rollback")
    void shouldPersistAmlAlertAfterDepositLimitRollback() {
        UUID accountId = account.getId();

        assertThrows(
                DepositLimitExceededException.class,
                () -> checkingAccountService.deposit(
                        accountId,
                        new WithdrawRequestDTO(accountId, BigDecimal.valueOf(50001), EnumOrigin.DINHEIRO_FISICO)
                )
        );

        List<AlertAML> alerts = alertsFor(accountId);
        assertEquals(1, alerts.size(), "O alerta de AML deveria ter sido persistido");
        AlertAML alert = alerts.get(0);
        assertEquals(0, alert.getAmount().compareTo(BigDecimal.valueOf(50001)));
        assertEquals(EnumStatusAlert.NOVO, alert.getStatus());

        assertEquals(
                0,
                checkingAccountRepository.findById(accountId).orElseThrow().getBalance()
                        .compareTo(BigDecimal.valueOf(1000)),
                "O depósito bloqueado não pode alterar o saldo"
        );
    }

    @Test
    @DisplayName("alerta e transação de fraude devem persistir após o rollback")
    void shouldPersistFraudAlertAndTransactionAfterRollback() {
        UUID accountId = account.getId();
        BigDecimal suspiciousAmount = BigDecimal.valueOf(1000);

        for (int i = 3; i >= 1; i--) {
            Transaction transaction = new Transaction();
            transaction.setAccountSends(accountId);
            transaction.setAccountReceive(accountId);
            transaction.setTransactionType(EnumTransactionType.DEPOSITO);
            transaction.setAmount(suspiciousAmount);
            transaction.setTransactionDate(LocalDateTime.now().minusSeconds(i * 30L));
            transaction.setStatus(EnumStatus.CONCLUIDA);
            transaction.setDescription("Depósito histórico " + i);
            transactionRepository.save(transaction);
        }

        assertThrows(
                IllegalStateException.class,
                () -> checkingAccountService.deposit(
                        accountId,
                        new WithdrawRequestDTO(accountId, suspiciousAmount, EnumOrigin.DINHEIRO_FISICO)
                )
        );

        List<AlertAML> alerts = alertsFor(accountId);
        assertFalse(alerts.isEmpty(), "O alerta de fraude deveria ter sido persistido");
        assertTrue(
                alerts.stream().anyMatch(a -> a.getStatus() == EnumStatusAlert.EM_ANALISE),
                "O alerta de fraude deve estar em análise"
        );

        assertTrue(
                transactionRepository.findAll().stream()
                        .anyMatch(t -> accountId.equals(t.getAccountSends())
                                && t.getStatus() == EnumStatus.PENDENTE
                                && t.getDescription() != null
                                && t.getDescription().contains("3 valores iguais")),
                "A transação de fraude deveria ter sido persistida fora do rollback"
        );

        assertEquals(
                0,
                checkingAccountRepository.findById(accountId).orElseThrow().getBalance()
                        .compareTo(BigDecimal.valueOf(1000)),
                "O depósito suspeito não pode alterar o saldo"
        );
    }

    private List<AlertAML> alertsFor(UUID accountId) {
        return alertAMLRepository.findAll().stream()
                .filter(a -> accountId.toString().equals(a.getAccountId()))
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
